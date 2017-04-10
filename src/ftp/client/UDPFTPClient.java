package ftp.client;

import ftp.FTPService;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
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

    private ExecutorService executor;

    // objetcs for communication
    private DatagramSocket s;
    private DatagramPacket packet;

    // data read from serverfile
    private File serverFile;
    private int nServers;
    ArrayList<InetAddress> serverList;
    ArrayList<Integer> serverPorts;
    ArrayList<Integer> serverBW;

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

        int i;
        String sRX = ""; //mensaje que recibimos
        String sTX; //mensaje que enviamos

        this.serverFile = new File(serverFilename);

        try {

            this.s = new DatagramSocket(lport);
            this.s.setSoTimeout(FTPService.TIMEOUT);
            this.packet = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            helloAction();

            if (this.serverList.size() != 0) {

                while (!sRX.equals(FTPService.Response.BYE.toString())) {

                    System.out.print("ftp>");
                    sTX = this.input.nextLine();
                    this.com = FTPService.commandFromString(sTX); //cogemos comando

                    sendCommand(sTX);
                }
            } else {
                System.out.println("No running servers found");
                System.exit(1);
            }

        } catch (Exception ax) {
            System.out.println("Exception caught while running ftp.client: \n"
                    + ax + "\nexiting");
            ax.printStackTrace();
            System.out.println(ax.getMessage());
            System.exit(2);
        } finally {

            System.out.println("Closing FTP session...");
            this.s.close();
        }
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

        nServers = 0;
        Pattern pat = Pattern.compile(pattern);

        serverList = new ArrayList<InetAddress>();
        serverPorts = new ArrayList<Integer>();
        serverBW = new ArrayList<Integer>();
        nServers = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            while ((line = br.readLine()) != null) {

                m = pat.matcher(line);
                if (m.find()) {
                    serverList.add(InetAddress.getByName(m.group(1)));
                    serverPorts.add(Integer.parseInt(m.group(2)));
                    serverBW.add(Integer.parseInt(m.group(3)));
                    nServers++;
                } else {
                    System.out.printf("Bad format line %d: %s\n", nServers + 1, line);
                }
            }

            if (nServers == 0) {
                System.out.println("No servers found, exiting");
                System.exit(1);
            }

        } catch (UnknownHostException uhx) {
            System.out.println("Error parsing server name: " + nServers);
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

                case QUIT:
                    this.quitAction();
                    break;

                default:
                    System.out.println("Unknown command");
                    break;
            }
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
        }

    }

    private void getTCP(Socket stream, String fileName) throws Exception {

        byte buff[] = new byte[10000000];
        File file = new File(fileName);
        InputStream dataStream = stream.getInputStream();
        FileOutputStream fOut = new FileOutputStream(file);
        int dataLength;
        while ((dataLength = dataStream.read(buff, 0, buff.length)) != -1) {
            fOut.write(buff, 0, dataLength);
            //System.out.println(buff.toString());
        }
        fOut.flush();

        fOut.close();
        dataStream.close();

    }

    private void getAction(String sTX) throws IOException {

        String fileStr = FTPService.requestedFile(sTX);
        File f = new File(fileStr);

        if (f.exists()) {
            System.out.print("file " + fileStr + " exists, do you want to overwrite it?\n>");
            if (! this.input.nextLine().equals("YES")){

                //TODO: load balance here or launch tcp handler to load balance

            }
        }



    }

    private void helloAction() {
        int i;

        parseServerInfo(this.serverFile);

        for (i = 0; i < this.serverList.size(); i++) {
            if (!sendHello(this.serverList.get(i), this.serverPorts.get(i))) {
                this.deleteServer(i);
                i--;
            }
        }
    }

    private void listAction() {
        int i;
        String receive = "";
        ArrayList<Integer> rPort = new ArrayList<Integer>();

        for (i = 0; i < this.serverList.size(); i++) {
            try {
                FTPService.sendUDPmessage(s,
                        FTPService.Command.LIST.toString(),
                        this.serverList.get(i),
                        this.serverPorts.get(i));

                s.receive(packet);

                receive = FTPService.stringFromDatagramPacket(packet);

                System.out.println("< " + FTPService.stringFromDatagramPacket(packet));
            } catch (IOException e) {
                System.out.println(e.getStackTrace().toString());
            }

            if (FTPService.responseFromString(receive)  == FTPService.Response.PORT) {
                rPort.add(FTPService.portFromResponse(receive));
                System.out.println("< Get port " + rPort.get(i));
            } else {
                deleteServer(i);
                i--;
            }
        }

        for (i = 0;  i < this.serverList.size(); i++) {
            executor.execute(new ClientListHandler(0,
                    this.serverList.get(i),
                    rPort.get(i),i));
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

        for (String entry: ClientMonitor.getMergedList()) {
            System.out.println(entry);
        }



    }
    private void quitAction() {}

    private void addServer(InetAddress rHost, int rPort, int BW) {

        this.serverList.add(rHost);
        this.serverPorts.add(rPort);
        this.serverBW.add(BW);
        this.nServers++;
    }

    private void deleteServer(int nServ) {

        this.serverList.remove(nServ);
        this.serverPorts.remove(nServ);
        this.serverBW.remove(nServ);
        this.nServers--;

    }
}