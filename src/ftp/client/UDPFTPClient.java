package ftp.client;

import ftp.FTPService;
import ftp.client.Session.RemoteServer;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Scanner;
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

    public static ExecutorService executor;

    // objetcs for communication
    private DatagramSocket s;
    private DatagramPacket packet;

    // data read from serverfile
    private File serverFile;
    ArrayList<RemoteServer> serverList;

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

    private void parseServerInfo(File f) {
        /**
         * Reads server file and extracts hostname/ip, port and expected bandwith
         */
        // TODO: make sure error messages and exceptions are coherent
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
     * @param rHost
     * @param rPort
     * @return
     */
    private boolean sendHello(InetAddress rHost, int rPort) {

        String receive;
        try {
            FTPService.sendUDPmessage(s, FTPService.Command.HELLO.toString(), rHost, rPort);
            s.receive(packet);

            receive = FTPService.stringFromDatagramPacket(packet);

            System.out.println ("< " + FTPService.stringFromDatagramPacket(packet));


            if (FTPService.responseFromString(receive)  != FTPService.Response.WCOME) {
                return false;
            }

        } catch (SocketTimeoutException ste) {
            System.out.println("Server " + rHost.getHostName() + ":" +
                    " appears to be down or not responding");
            System.out.println(ste.getStackTrace().toString()) ;
        } catch (IOException gx) {
            System.out.println(gx.getStackTrace().toString()) ;
            return false;
        }
        return true;
    }

    /**
     *
     * @param command
     */
    private void sendCommand(String command) {

        FTPService.Command cRX = FTPService.commandFromString(command);
        try {
            switch (cRX) {
                case HELLO:
                    this.helloAction();
                    break;

                case LIST:
                    this.listAction();
                    break;

                case GET:
                    this.getAction(command);
                    break;

                case CHECKBW:
                    this.checkBWAction();
                    break;

                case QUIT:
                    this.quitAction();
                    break;

                default:
                    System.out.println("Unknown command");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }

    private void getAction(String command) throws IOException {

        int i;
        String receive = "";
        ArrayList<Integer> rPortTCP = new ArrayList<Integer>();
        Pattern p;
        Matcher m;
        int fileSize = 0;
        String reqFile;
        File file;

        ArrayList<String> list = ClientMonitor.getMergedList();
        //sacamos info de archivos de LIST que ya se habra hecho

        reqFile = FTPService.requestedFile(command);
        //sacamos archivo que queremos

        p = Pattern.compile("(.*)\\t(\\d+)$");
        //buscamos en el list


        for(i = 0; i < list.size() && fileSize == 0; i++) {
            m = p.matcher(list.get(i));
            if (m.find() && m.group(1).equals(reqFile)) {
                fileSize = Integer.parseInt(m.group(2));
                //buscamos archivo en la lista que tiene el nombre del pedido
            }
        }

        if ( fileSize != 0 ) { //nos aseguramos de que archivo existe

            String[] intervalStr = getInterval(fileSize); //obtenemos strings de partes
            String[] parts = getInterval(fileSize);
            int intval[];

            try {
                for (i = 0; i < this.serverList.size(); i++) {

                        FTPService.sendUDPmessage(s,
                                FTPService.Command.GET.toString() + " " + reqFile + intervalStr[i],
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
                    intval = FTPService.getIntervalFromPart(reqFile + parts[i]);
                    executor.execute(new ClientFileHandler(0,
                            this.serverList.get(i).getAddr(),
                            rPortTCP.get(i), i,
                            Files.createFile(Paths.get(reqFile + parts[i])).toFile(),
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

                file = Files.createFile(Paths.get(reqFile)).toFile();
                FileOutputStream fOut = new FileOutputStream(file);
                FileInputStream fIn;
                byte[] data = new byte[FTPService.CHUNKSIZE];
                int dataLength;
                File part;
                for (i = 0;  i < this.serverList.size(); i++) {
                    part = Paths.get(reqFile + parts[i]).toFile();
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
        int i;

        parseServerInfo(this.serverFile);

        for (i = 0; i < this.serverList.size(); i++) {
            if (!sendHello(this.serverList.get(i).getAddr(), this.serverList.get(i).getPort())) {
                this.deleteServer(i);
                i--;
            }
        }
    }

    private void listAction() {
        int i;
        String receive = "";
        ArrayList<Integer> rPortsTCP = new ArrayList<Integer>();

        for (i = 0; i < this.serverList.size(); i++) {
            try {
                FTPService.sendUDPmessage(s,
                        FTPService.Command.LIST.toString(),
                        this.serverList.get(i).getAddr(),
                        this.serverList.get(i).getPort());

                s.receive(packet);

                receive = FTPService.stringFromDatagramPacket(packet);

                System.out.println("< " + FTPService.stringFromDatagramPacket(packet));
            } catch (IOException e) {
                System.out.println(e.getStackTrace().toString());
            }

            if (FTPService.responseFromString(receive)  == FTPService.Response.PORT) {
                rPortsTCP.add(FTPService.portFromResponse(receive));
                System.out.println("< Get port " + rPortsTCP.get(i));
            } else {
                deleteServer(i);
                i--;
            }
        }

        for (i = 0;  i < this.serverList.size(); i++) {
            executor.execute(new ClientListHandler(0,
                    this.serverList.get(i).getAddr(),
                    rPortsTCP.get(i),i));
        }

        for (i = 0;  i < this.serverList.size(); i++) {
            try {
                s.receive(packet);
                System.out.println("< " + FTPService.stringFromDatagramPacket(packet)
                        + " from " + packet.getAddress().getHostAddress()
                        + ":" + packet.getPort());
            } catch (IOException e) {
                System.out.println(e.getStackTrace().toString());
            }

        }

        try {
            ArrayList<String> list = ClientMonitor.getMergedList();
            for (String entry : list) {
                System.out.println(entry);
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }

    private void checkBWAction(){

        int i;
        String receive = "";
        ArrayList<Integer> rPortsTCP = new ArrayList<Integer>();

        for (i = 0; i < this.serverList.size(); i++) {
            try {
                FTPService.sendUDPmessage(s,
                        FTPService.Command.CHECKBW.toString(),
                        this.serverList.get(i).getAddr(),
                        this.serverList.get(i).getPort());

                s.receive(packet);

                receive = FTPService.stringFromDatagramPacket(packet);

                System.out.println("< " + FTPService.stringFromDatagramPacket(packet));
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            if (FTPService.responseFromString(receive)  == FTPService.Response.PORT) {
                rPortsTCP.add(FTPService.portFromResponse(receive));
                serverList.get(i).getBandWidth(rPortsTCP.get(i));
            } else {
                deleteServer(i);
                i--;
            }
        }

        for (i = 0;  i < this.serverList.size(); i++) {
            try {
                s.setSoTimeout(0);
                s.receive(packet);
                System.out.println("< " + FTPService.stringFromDatagramPacket(packet)
                        + " from " + packet.getAddress().getHostAddress()
                        + ":" + packet.getPort());
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

        }

        for (i = 0;  i < this.serverList.size(); i++) {

            if (!this.serverList.get(i).isUP()){
                this.serverList.remove(i);
                i--;
            }
        }
    }

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

    private void quitAction() {
        int i;

        for (i = 0; i < this.serverList.size(); i++) {
            String receive;
            try {
                FTPService.sendUDPmessage(s, FTPService.Command.QUIT.toString(), this.serverList.get(i).getAddr(), this.serverList.get(i).getPort());
                s.receive(packet);

                receive = FTPService.stringFromDatagramPacket(packet);

                System.out.println ("< " + FTPService.stringFromDatagramPacket(packet));

            } catch (SocketTimeoutException ste) {
                System.out.println(ste.getStackTrace().toString()) ;
            } catch (IOException gx) {
                System.out.println(gx.getStackTrace().toString()) ;
            }
        }
    }

    private void addServer(InetAddress rHost, int rPort, int BW) {

        this.serverList.add(new RemoteServer(rHost, rPort, BW));
    }

    private void deleteServer(int nServ) {

        this.serverList.remove(nServ);

    }
}