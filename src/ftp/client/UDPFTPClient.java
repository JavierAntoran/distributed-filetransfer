package ftp.client;

import ftp.FTPService;
import ftp.client.Session.RemoteFile;
import ftp.client.Session.RemoteServer;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class UDPFTPClient {

    static final String SERVERFILE = "ftp_files/servers.txt";

    Scanner input = new Scanner(System.in);

    private FTPService.Command com; //Last sent command

    private ExecutorService executor;

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

        executor = Executors.newFixedThreadPool(FTPService.MAXSERVERTHREADS);

        String sRX = ""; //mensaje que recibimos
        String sTX; //mensaje que enviamos

        this.serverFile = new File(serverFilename);

        try {

            this.s = new DatagramSocket(lport);
            this.s.setSoTimeout(FTPService.TIMEOUT);
            this.packet = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            helloAction();

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
        // TODO: Allow tab as a separator
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
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            return false;
        } catch (IOException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            return false;
        }
        return true;
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

        int i;
        String receive = "";
        ArrayList<Integer> rPortTCP = new ArrayList<Integer>();

        Matcher m;
        int fileSize = 0;
        String reqFilename;
        File file;

        HashMap<String, RemoteFile> fileList = ClientMonitor.getMergedList();
        //sacamos info de archivos de LIST que ya se habra hecho

        reqFilename = FTPService.requestedFile(command);
        //sacamos archivo que queremos

        if ( fileList.containsKey(reqFilename) ) { //nos aseguramos de que archivo existe

            RemoteFile reqFile = fileList.get(reqFilename);

            ArrayList<String> intervalStr = getInterval(reqFile.getFileSize()); //obtenemos strings de partes
            ArrayList<String> parts = getInterval(fileSize);
            int intval[];

            try {
                for (i = 0; i < this.serverList.size(); i++) {

                        FTPService.sendUDPmessage(s,
                                FTPService.Command.GET.toString() + " " + reqFile + intervalStr.get(i),
                                this.serverList.get(i).getAddr(),
                                this.serverList.get(i).getPort()); //enviamos peticion

                        s.receive(packet); //recibimos respuesta

                        receive = FTPService.stringFromDatagramPacket(packet);

                        System.out.println("< " + FTPService.stringFromDatagramPacket(packet));

                        if (FTPService.responseFromString(receive)  == FTPService.Response.PORT) {
                            rPortTCP.add(FTPService.portFromResponse(receive));
                            System.out.println("< Get port " + rPortTCP.get(i));
                        }

                }

                for(i = 0;  i < this.serverList.size(); i++) {
                    intval = FTPService.getIntervalFromPart(reqFile + parts.get(i));
                    executor.execute(new ClientFileHandler(0,
                            this.serverList.get(i).getAddr(),
                            rPortTCP.get(i), i,
                            Files.createFile(Paths.get(reqFile + parts.get(i))).toFile(),
                            intval[0], intval[1]));
                }

                s.setSoTimeout(0);
                for (i = 0;  i < this.serverList.size(); i++) {
                    try {
                        s.receive(packet);
                        System.out.println("< " + FTPService.stringFromDatagramPacket(packet)
                                + " from " + packet.getAddress().getHostAddress()
                                + ":" + packet.getPort());
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e.getMessage());
                    }

                }

                file = Files.createFile(Paths.get(reqFile.getFileName())).toFile();
                FileOutputStream fOut = new FileOutputStream(file);
                FileInputStream fIn;
                byte[] data = new byte[FTPService.CHUNKSIZE];
                int dataLength;
                File part;
                for (i = 0;  i < this.serverList.size(); i++) {
                    part = Paths.get(reqFile + parts.get(i)).toFile();
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

        ArrayList<Integer> rPorts = new ArrayList<Integer>();
        ArrayList<String> partsExts = new ArrayList<String>();

        ArrayList<RemoteServer> rsOK = new ArrayList<RemoteServer>();
        ArrayList<RemoteServer> rsERR = new ArrayList<RemoteServer>();

        ArrayList<Thread> clhList = new ArrayList<Thread>();

        try {
            ClientMonitor.resetList();

            for (RemoteServer server : this.serverList) {
                try {
                    FTPService.sendUDPmessage(this.s,
                            FTPService.Command.LIST.toString(),
                            server.getAddr(),
                            server.getPort());

                    this.s.receive(packet);

                    response = FTPService.stringFromDatagramPacket(packet);

                    FTPService.logInfo(String.format("< [%s] %s ", server, response));

                    if (FTPService.responseFromString(response) == FTPService.Response.PORT) {
                        Thread clhT = new Thread(new ClientListHandler(0,
                                server.getAddr(),
                                FTPService.portFromResponse(response),
                                server.hashCode()));
                        clhT.start();
                        clhList.add(clhT);

                    } else {
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

                    FTPService.logInfo(String.format("< [%s] %s ", server, response));

                    if (FTPService.responseFromString(response) != FTPService.Response.OK) {
                        FTPService.logWarn(String.format("Unexpected LIST response from %s", server.getName()));
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
                    System.out.println(String.format("%1$8s\t%2$s", rFile.getFileSize(), rFile.getFileName()));
                }
            } else {
                System.out.println("Directory empty");
            }
        } catch (ConcurrentModificationException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
        }

    }

    /**
     * Returns the part extension for each remote server based on
     * available bandwidth
     * @param int fileSize
     * @return String[]
     */
    private ArrayList<String> getInterval(long fileSize) {

        ArrayList<String> partsExts = new ArrayList<String>();
        int totalBW = 0;
        int relativeBW = 0;
        int chunkOffset = 0;
        long nChunks = FTPService.getNChunks(fileSize, FTPService.CHUNKSIZE);

        for(RemoteServer server: this.serverList) {
            totalBW += server.getBw();
        }

        for(RemoteServer server: this.serverList) {
            relativeBW = Math.round(nChunks * server.getBw() / totalBW);

            partsExts.set(this.serverList.indexOf(server),
                    ".part" + (chunkOffset + 1) + "-" + (relativeBW + chunkOffset));
            chunkOffset += relativeBW;
        }

        if (chunkOffset - nChunks > 0) {
            partsExts.set(this.serverList.size() - 1,
                    ".part" + (chunkOffset - relativeBW + 1) + "-" + (chunkOffset - 1) );
        } else if (chunkOffset - nChunks < 0) {
            partsExts.set(this.serverList.size() - 1,
                    ".part" + (chunkOffset - relativeBW + 1) + "-" + (chunkOffset + 1) );
        }

        return partsExts;
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