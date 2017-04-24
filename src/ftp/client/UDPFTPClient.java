package ftp.client;

import ftp.FTPService;
import ftp.client.Session.RemoteFile;
import ftp.client.Session.RemoteServer;
import ftp.client.tcp.ClientFileHandler;
import ftp.client.tcp.ClientListHandler;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class UDPFTPClient {

    static final String SERVERFILE = "ftp_files/servers.txt";

    Scanner input = new Scanner(System.in);

    private FTPService.Command com; //Last sent command


    // objetcs for communication
    private DatagramSocket s;
    private DatagramPacket packet;

    // data read from serverfile
    private File serverFile;
    ArrayList<RemoteServer> serverList;
    HashMap<String, RemoteFile> fileList;

    public static void main(String[] args) {

        switch (args.length) {
            case 0:
                new UDPFTPClient(0, SERVERFILE);
                break;
            case 1:
                new UDPFTPClient(0, args[0]);
                break;
            case 2:
                new UDPFTPClient(Integer.parseInt(args[1]), args[0]);
                break;
            default:
                System.out.println("invalid number of arguments");
                System.exit(2);
        }

    }

    public UDPFTPClient(int lport, String serverFilename) {

        String sRX = ""; //mensaje que recibimos
        String sTX; //mensaje que enviamos

        this.serverFile = new File(serverFilename);

        try {

            this.s = new DatagramSocket(lport);
            this.s.setSoTimeout(FTPService.TIMEOUT);
            this.packet = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            helloAction(); //parse file and send Hello

            if (this.serverList.size() != 0) {

                while (! (this.com == FTPService.Command.QUIT)) {

                    System.out.print("ftp>");
                    sTX = this.input.nextLine();
                    this.com = FTPService.commandFromString(sTX); //cogemos comando

                    sendCommand(sTX);
                }
            } else {
                System.out.println("No running servers found");
                System.exit(1);
            }

            this.s.close();

        } catch (Exception ax) {
            System.out.println("Exception caught while running ftp.client: \n"
                    + ax + "\nexiting");
            ax.printStackTrace();
            System.out.println(ax.getMessage());
            System.exit(2);
        }
        System.out.println("Closing FTP session...");
        System.exit(0);
    }

    /**
     * Reads server file and extracts hostname/ip, port and expected bandwith
     *
     * @param File f Server file list
     */
    private void parseServerInfo(File f) {

        // TODO: make sure error messages and exceptions are coherent
        // TODO: if server objects already created dont override bandwidth

        String pattern = ".*\\s+(.*)\\s+(\\d+)\\s+(\\d+)$";
        String line;
        Matcher m;

        if (!f.exists() || f.isDirectory()) {
            System.out.println("No se ha encontrado archivo de info de servidores");
            System.exit(1);
        }

        Pattern pat = Pattern.compile(pattern);

        serverList = new ArrayList<RemoteServer>();


        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            while ((line = br.readLine()) != null) {

                m = pat.matcher(line);
                if (m.find()) {
                    serverList.add(new RemoteServer(InetAddress.getByName(m.group(1)),
                            Integer.parseInt(m.group(2)),
                            Integer.parseInt(m.group(3))));
                } else {
                    System.out.printf("Bad format line %d: %s\n", serverList.size(), line);
                }
            }

            if (serverList.size() == 0) {
                System.out.println("No servers found, exiting");
                System.exit(1);
            }

        } catch (UnknownHostException uhx) {
            System.out.println("Error parsing server name: " + serverList.size());
            System.out.println(uhx.getStackTrace().toString());
        } catch (IOException iox) {
            System.out.println("Error reading server data file");
            System.out.println(iox.getStackTrace().toString());
        }

    }

    /**
     * Acts both as a HELLO protocol and connection test
     * returns true if server is up, false if remote host times out
     * or another @IOException occurs
     *
     * @param RemoteServer rs
     * @return boolean
     */
    private boolean sendHello(RemoteServer rs) {
        String response;
        boolean isUp = true;
        try {

            FTPService.sendUDPmessage(s, FTPService.Command.HELLO.toString(),
                    rs.getAddr(), rs.getPort());

            this.s.receive(packet);

            response = FTPService.stringFromDatagramPacket(packet);


            FTPService.logInfo(String.format("< [%s] %s ", rs, response));


            if (FTPService.responseFromString(response) != FTPService.Response.WCOME) {
                FTPService.logWarn(String.format("Unexpected HELLO response from %s", rs.getName()));
                return false;
            }

        } catch (SocketTimeoutException e) {
            FTPService.logWarn(String.format("Server %s appears to be down or not responding",  rs.getName()));
            isUp = false;
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            return false;
        } catch (IOException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            return false;
        }
        return isUp;
    }

    /**
     *  Executes the specified command action
     *
     * @param String command
     */
    private void sendCommand(String command) {

        FTPService.Command c = FTPService.commandFromString(command);
        try {
            switch (c) {
                case HELLO:
                    this.helloAction();
                    break;

                case LIST:
                    this.listAction();
                    break;

                case GET:
                    this.getAction(command);
                    break;

                /*case CHECKBW:
                    this.checkBWAction();
                    break;
                    */

                case QUIT:
                    this.quitAction();
                    break;

                default:
                    System.out.println("Unknown command, try: " + FTPService.Command.values().toString());
                    break;
            }
        } catch (IOException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
        }

    }

    private void getAction(String command) throws IOException {

        String response;

        ArrayList<Thread> cfhList = new ArrayList<Thread>();

        int fileSize = 0;
        String reqFilename;
        File file;

        // Retrieve server file list
        HashMap<String, RemoteFile> fileList = ClientMonitor.getMergedList();

        // Requested filename
        reqFilename = FTPService.requestedFile(command);

        // Check file exists in servers
        if ( fileList.containsKey(reqFilename) ) {

            // Get RemoteFile instance
            RemoteFile reqFile = fileList.get(reqFilename);


            ArrayList<String> partsPerServer = reqFile.getPartsPerServer(this.serverList);
            int intval[];

            try {
                for (RemoteServer server : this.serverList) {
                    String serverFilePart = reqFilename + partsPerServer.get(this.serverList.indexOf(server));
                    try {
                        FTPService.sendUDPmessage(this.s,
                                String.format("%s %s",FTPService.Command.GET.toString(),
                                              serverFilePart ),
                                server.getAddr(),
                                server.getPort());

                        this.s.receive(packet);

                        response = FTPService.stringFromDatagramPacket(packet);

                        FTPService.logInfo(String.format("< [%s] %s ", server, response));

                        if (FTPService.responseFromString(response) == FTPService.Response.PORT) {
                            intval = FTPService.getIntervalFromPart(serverFilePart);

                            Thread cfhT = new Thread(
                                    new ClientFileHandler(
                                            0,
                                            server,
                                            FTPService.portFromResponse(response),
                                            server.hashCode(),
                                            Files.createFile(Paths.get(serverFilePart)).toFile(),
                                            intval[0],
                                            intval[1]));
                            cfhT.start();
                            cfhList.add(cfhT);

                        } else {
                            this.serverList.remove(server);
                        }

                    } catch (IOException e) {
                        FTPService.logErr(e.getMessage());
                        FTPService.logDebug(e);
                    }
                }

                for (Thread cfh : cfhList) {
                    try {
                        cfh.join();
                    }catch (InterruptedException e) {
                        FTPService.logErr(e.getMessage());
                        FTPService.logDebug(e);
                    }
                }

                for (RemoteServer server : this.serverList) {
                    try {
                        s.receive(packet);

                        response = FTPService.stringFromDatagramPacket(packet);

                        FTPService.logInfo(String.format("< [%s] %s ", server, response));

                        if (FTPService.responseFromString(response) != FTPService.Response.OK) {
                            FTPService.logWarn(String.format("Unexpected LIST response from %s", server.getName()));
                        }
                    } catch (IOException e) {
                        FTPService.logErr(e.getMessage());
                        FTPService.logDebug(e);
                    }
                }

                file = Files.createFile(Paths.get(reqFile.getFileName())).toFile();
                FileOutputStream fOut = new FileOutputStream(file);
                FileInputStream fIn;
                byte[] data = new byte[FTPService.CHUNKSIZE];
                int dataLength;
                File part;
                for (int i = 0;  i < this.serverList.size(); i++) {
                    part = Paths.get(reqFilename + partsPerServer.get(i)).toFile();
                    fIn = new FileInputStream(part);
                    while ( (dataLength = fIn.read(data)) != -1) {
                        fOut.write(data,0,dataLength);
                    }
                    fIn.close();
                    part.delete();
                }
                fOut.close();


            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }

    }

    private void helloAction() {

        parseServerInfo(this.serverFile);

        for (RemoteServer server: this.serverList) {
            if (!sendHello(server) ) {
                this.serverList.remove(server);
            }
        }

    }

    private void listAction() {

        String response;

        ArrayList<Thread> clhList = new ArrayList<Thread>();

        ClientMonitor.resetList();

        for (RemoteServer server : this.serverList) {
            try {
                FTPService.sendUDPmessage(this.s,
                        FTPService.Command.LIST.toString(),
                        server.getAddr(),
                        server.getPort());

                this.s.receive(packet);

                response = FTPService.stringFromDatagramPacket(packet);

                FTPService.logInfo(String.format("< [%s:%s] %s ",
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), response));

                if (FTPService.responseFromString(response) == FTPService.Response.PORT) {
                    Thread clhT = new Thread(new ClientListHandler(0,
                            server,
                            FTPService.portFromResponse(response),
                            server.hashCode()));
                    FTPService.logWarn(String.format("Running thread %s for %s", clhT.getId(), server.getName()));
                    clhT.start();
                    clhList.add(clhT);
                } else {
                    FTPService.logWarn(String.format("Unexpected PORT response from %s", server.getName()));
                    FTPService.logWarn(String.format("Deleting % server form available servers", server.getName()));
                    this.serverList.remove(server);
                }

            } catch (IOException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }

        for (Thread clh : clhList) {
            try {
                clh.join();
            }catch (InterruptedException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }

        for (RemoteServer server : this.serverList) {
            try {
                s.receive(packet);

                response = FTPService.stringFromDatagramPacket(packet);

                FTPService.logInfo(String.format("< [%s:%s] %s ",
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), response));

                if (FTPService.responseFromString(response) != FTPService.Response.OK) {
                    FTPService.logWarn(String.format("Unexpected LIST response from %s:%s",
                            packet.getAddress().getHostAddress(),
                            packet.getPort()));
                }
            } catch (IOException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }

        this.fileList = ClientMonitor.getMergedList();

        if (this.fileList.size() > 0) {
            System.out.println("Size    \tFilename");
            System.out.println("--------------------");
            for (Map.Entry<String, RemoteFile> entry : fileList.entrySet()) {
                RemoteFile rFile = entry.getValue();
                System.out.println(
                        String.format("%1$8s\t%2$s",
                                rFile.getFileSize(),
                                rFile.getFileName()));
            }
        } else {
            System.out.println("Directory empty");
        }

    }

    /**
     * function is deprecated, will be deleted
     * @param fileSize
     * @return
     */
    private String[] getInterval(int fileSize) {

        int i;
        String interval[] = new String[serverList.size()];
        int totalBW = 0;
        int relativeBW = 0;
        int chunkOffset = 0;
        long nChunks = FTPService.getNChunks(fileSize, FTPService.CHUNKSIZE);

        for (i = 0; i < serverList.size(); i++) {
            totalBW += serverList.get(i).getBw();
        }

        for (i = 0; i < serverList.size(); i++) {
            relativeBW = Math.round(nChunks * serverList.get(i).getBw() / totalBW);
            interval[i] = ".part" + (chunkOffset + 1) + "-" + (relativeBW + chunkOffset);
            chunkOffset += relativeBW;
        }

        if (chunkOffset - nChunks > 0) {
            interval[serverList.size() - 1] = ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset - 1);
        } else if (chunkOffset - nChunks > 0) {
            interval[serverList.size() - 1] = ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset + 1);
        }

        return interval;


    }

    /**
     * Executes QUIT call to remote servers
     */
    private void quitAction() {
        String response;

        for (RemoteServer server: this.serverList) {
            try {
                FTPService.sendUDPmessage(
                        s,
                        FTPService.Command.QUIT.toString(),
                        server.getAddr(),
                        server.getPort());

                s.receive(packet);

                response = FTPService.stringFromDatagramPacket(packet);

                FTPService.logInfo(String.format("< %", FTPService.stringFromDatagramPacket(packet)));

                if (FTPService.responseFromString(response) != FTPService.Response.WCOME) {
                    FTPService.logWarn(String.format("Unexpected QUIT response from %s", server.getName()));
                }

            } catch (SocketTimeoutException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            } catch (IOException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }
    }

}