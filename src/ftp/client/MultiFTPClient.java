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
public class MultiFTPClient {

    static final String SERVERFILE = "servers.txt";

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
                System.out.println(String.format("Using %s as server list file", SERVERFILE));
                new MultiFTPClient(0, SERVERFILE);
                break;
            case 1:
                new MultiFTPClient(0, args[0]);
                break;
            case 2:
                new MultiFTPClient(Integer.parseInt(args[1]), args[0]);
                break;
            default:
                System.out.println("invalid number of arguments");
                System.exit(2);
        }

    }

    public MultiFTPClient(int lport, String serverFilename) {

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
                    if (this.fileList.size() > 0) {
                        System.out.println("Size    \tFilename");
                        System.out.println("--------------------");
                        for (Map.Entry<String, RemoteFile> entry : this.fileList.entrySet()) {
                            RemoteFile rFile = entry.getValue();
                            System.out.println(
                                    String.format("%1$8s\t%2$s",
                                            rFile.getFileSize(),
                                            rFile.getFileName()));
                        }
                    } else {
                        System.out.println("Directory empty");
                    }
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
                    System.out.println("Unknown command, try: HELLO, LIST, GET, QUIT" );
                    break;
            }
        } catch (IOException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
        }

    }

    private void getAction(String command) throws IOException {

        String response;

        ArrayList<Thread> cfhThreadList;
        ArrayList<ClientFileHandler> cfhList;
        ArrayList<RemoteServer> fileRS;
        ArrayList<RemoteServer> upFileRS;
        ArrayList<RemoteServer> downFileRS;

        String reqFilename;

        int nChunks;
        Integer[] chunks;
        int i;
        int receivedChunks = 0;

        // Retrieve server file list
        HashMap<String, RemoteFile> fileList = ClientMonitor.getMergedList();

        // Requested filename
        reqFilename = FTPService.requestedFile(command);

        // Refresh file list
        if (this.fileList == null || this.fileList.size() == 0) {
            this.listAction();
        }

        // Check file exists in servers
        if ( this.fileList.containsKey(reqFilename) ) {

            if (Files.exists(Paths.get(reqFilename))) {
                System.out.print(String.format("The file \"%s\" exists. Replace it? Y/N: ",reqFilename ));
                String answer = this.input.nextLine();
                if (answer.equalsIgnoreCase("N")) {
                    return;
                }
            }

            // Get RemoteFile instance
            RemoteFile reqFile = this.fileList.get(reqFilename);

            nChunks = FTPService.getNChunks(reqFile.getFileSize(), FTPService.CHUNKSIZE);

            chunks = new Integer[nChunks];
            for(i = 0; i < nChunks; chunks[i] = i+1, i++);

            int[] chunksPerServer;

            Files.deleteIfExists(Paths.get(reqFilename));
            Files.deleteIfExists(Paths.get(reqFilename+".part"));

            File partFile = Files.createFile(Paths.get(reqFilename+".part")).toFile();

            fileRS = reqFile.getServerList();

            while (receivedChunks < nChunks && fileRS.size() != 0) {
                cfhThreadList = new ArrayList<Thread>();
                cfhList = new ArrayList<ClientFileHandler>();
                ArrayList<Integer> serverTcpPorts = new ArrayList<Integer>();

                chunksPerServer = RemoteFile.distributeChunks(
                        fileRS,
                        reqFile.getFileSize(),
                        chunks);

                for (i=0; i<chunksPerServer.length; i=i+2){
                    FTPService.logDebug(String.format(
                            "Parts for server: %d-%d",
                            chunksPerServer[i],
                            chunksPerServer[i+1])
                    );
                }

                ArrayList<Integer> chunksLeft = new ArrayList<Integer>();

                downFileRS = new ArrayList<RemoteServer>();
                upFileRS = new ArrayList<RemoteServer>();

                for(RemoteServer server: fileRS) {

                    int srvIdx = fileRS.indexOf(server);

                    if (chunksPerServer[2*srvIdx] != 0 &&  chunksPerServer[2*srvIdx + 1] != 0) {

                        String serverFilePart = reqFilename +
                                String.format(".part%d-%d", chunksPerServer[2 * srvIdx], chunksPerServer[2 * srvIdx + 1]);

                        try {
                            FTPService.sendUDPmessage(this.s,
                                    String.format("%s %s", FTPService.Command.GET.toString(),
                                            serverFilePart),
                                    server.getAddr(),
                                    server.getPort());

                            this.s.receive(packet);

                            response = FTPService.stringFromDatagramPacket(packet);

                            FTPService.logInfo(String.format("< [%s:%s] %s ",
                                    packet.getAddress().getHostAddress(),
                                    packet.getPort(), response));

                            if (FTPService.responseFromString(response) == FTPService.Response.PORT) {
                                serverTcpPorts.add(FTPService.portFromResponse(response));
                                upFileRS.add(server);
                            } else {
                                FTPService.logWarn(String.format("Unexpected PORT response from %s", server.getName()));
                                FTPService.logWarn(String.format("Deleting %S server form available servers", server.getName()));
                                downFileRS.add(server);
                                for (i = chunksPerServer[2 * srvIdx]; i <= chunksPerServer[2 * srvIdx + 1]; i++) {
                                    chunksLeft.add(i);
                                }
                            }

                        } catch (IOException e) {
                            FTPService.logErr(e.getMessage());
                            FTPService.logDebug(e);
                            downFileRS.add(server);
                            for (i = chunksPerServer[2 * srvIdx]; i <= chunksPerServer[2 * srvIdx + 1]; i++) {
                                chunksLeft.add(i);
                            }
                        }
                    } else {
                        downFileRS.add(server);
                    }
                }



                for(RemoteServer server: upFileRS) {
                    int srvIdx = upFileRS.indexOf(server);

                    ClientFileHandler cfh = new ClientFileHandler(
                            0,
                            server,
                            serverTcpPorts.get(srvIdx),
                            server.hashCode(),
                            partFile,
                            chunksPerServer[2 * srvIdx], chunksPerServer[2 * srvIdx + 1]);
                    Thread cfhT = new Thread(cfh);
                    FTPService.logDebug(String.format("Running thread %s for %s", cfhT.getId(), server.getName()));
                    cfhT.start();
                    cfhList.add(cfh);
                    cfhThreadList.add(cfhT);
                }

                for (Thread cfhT : cfhThreadList) {
                    int cfhIdx = cfhThreadList.indexOf(cfhT);
                    ClientFileHandler cfhL = cfhList.get(cfhIdx);
                    RemoteServer server = upFileRS.get(cfhIdx);
                    try {
                        cfhT.join();
                        if (cfhL.getLastReceivedChunk() != cfhL.getLastChunk() ) {
                            for (i = cfhL.getLastReceivedChunk(); i<=cfhL.getLastChunk(); i++) {
                                System.out.println("ChunkLeft: " + i);
                                chunksLeft.add(i);
                            }
                            downFileRS.add(server);
                        }
                    } catch (InterruptedException e) {
                        FTPService.logErr(e.getMessage());
                        FTPService.logDebug(e);
                        downFileRS.add(server);
                    }
                    receivedChunks += cfhL.getLastReceivedChunk()-cfhL.getFirstChunk()+1;
                }

                chunks = new Integer[chunksLeft.size()];
                chunks = chunksLeft.toArray(chunks);

                for (RemoteServer server: downFileRS) {
                    upFileRS.remove(server);
                    fileRS.remove(server);
                }

                for(i = 0; i<fileRS.size(); i++) {
                    try {
                        s.receive(packet);

                        response = FTPService.stringFromDatagramPacket(packet);

                        FTPService.logInfo(String.format("< [%s:%s] %s ",
                                packet.getAddress().getHostAddress(),
                                packet.getPort(), response));

                        if (FTPService.responseFromString(response) != FTPService.Response.OK) {
                            FTPService.logWarn(String.format("Unexpected GET response from %s:%s",
                                    packet.getAddress().getHostAddress(),
                                    packet.getPort()));
                        }
                    } catch (IOException e) {
                        FTPService.logErr(e.getMessage());
                        FTPService.logDebug(e);
                    }
                }
            }

            if (partFile.length() == reqFile.getFileSize()) {
                Files.move(Paths.get(reqFilename+".part"), Paths.get(reqFilename));
                FTPService.logInfo("Transfer Complete");
            } else {
                FTPService.logInfo("An error ocurred during transfer. See debug details for more information");
                Files.deleteIfExists(Paths.get(reqFilename+".part"));
            }

        } else {
            System.out.println(String.format("File %s not found", reqFilename));
        }

    }

    private void helloAction() {

        parseServerInfo(this.serverFile);
        Iterator<RemoteServer> it = this.serverList.listIterator();
        while (it.hasNext()) {
            RemoteServer server = it.next();
            if (!sendHello(server) ) {
                it.remove();
            }
        }

    }

    private void listAction() {

        String response;

        ArrayList<Thread> clhList = new ArrayList<Thread>();

        ClientMonitor.resetList();
        ArrayList<Integer> serverTcpPorts = new ArrayList<Integer>();
        Iterator<Integer> serverTcpPortsIterator;
        Iterator<RemoteServer> serverListIterator =  this.serverList.listIterator();
        while ( serverListIterator.hasNext() ) {
            RemoteServer server = serverListIterator.next();

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
                    serverTcpPorts.add(FTPService.portFromResponse(response));
                } else {
                    FTPService.logWarn(String.format("Unexpected PORT response from %s", server.getName()));
                    FTPService.logWarn(String.format("Deleting %s server form available servers", server.getName()));
                        serverListIterator.remove();
                }

            } catch (IOException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }

        serverTcpPortsIterator =  serverTcpPorts.listIterator();
        while ( serverTcpPortsIterator.hasNext() ) {
            Integer tcpPort = serverTcpPortsIterator.next();
            RemoteServer server = this.serverList.get(serverTcpPorts.indexOf(tcpPort));
            Thread clhT = new Thread(new ClientListHandler(0,
                    server,
                    tcpPort,
                    server.hashCode()));
            FTPService.logDebug(String.format("Running thread %s for %s", clhT.getId(), server.getName()));
            clhT.start();
            clhList.add(clhT);
        }

        for (Thread clh : clhList) {
            try {
                clh.join();
            }catch (InterruptedException e) {
                FTPService.logErr(e.getMessage());
                FTPService.logDebug(e);
            }
        }

        serverListIterator =  this.serverList.listIterator();

        while (serverListIterator.hasNext()){
            serverListIterator.next();

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