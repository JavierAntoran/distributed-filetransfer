package ftp.client;

import ftp.FTPService;

import java.io.*;
import java.net.*;
import java.util.Scanner;
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
    private int nServers;
    private InetAddress serverList[];
    private int serverPorts[];
    private int serverBW[];

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

    public UDPFTPClient(int lport, String serverFile) {

        int i;
        String sRX = ""; //mensaje que recibimos
        String sTX; //mensaje que enviamos

        parseServerInfo(new File(serverFile));

        try {

            this.s = new DatagramSocket(lport);
            this.s.setSoTimeout(FTPService.TIMEOUT);
            this.packet = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            for (i = 0; i < this.nServers; i++) {// tries to establish conection with each server
                if (! sendHello(this.serverList[i], this.serverPorts[i])) {
                    //TODO: remove server from list if HELLO fails
                }
            }

            while (!sRX.equals( FTPService.Response.BYE.toString())) {

                System.out.print("\n>");
                sTX = this.input.nextLine();
                this.com = FTPService.commandFromString(sTX); //cogemos comando


                //FTPService.sendUDPmessage(s, sTX, rHost, rPort);//Hacemos peticion

                this.s.receive(this.packet);
                sRX = new String(this.packet.getData(),0, this.packet.getLength());
                this.packet.setLength(FTPService.SIZEMAX);

                handleResponse(sRX, this.packet.getAddress());
                //obtenemos respuesta como string y la interpretamos
            }

        } catch (Exception ax) {
            System.out.println("Exception caught while running ftp.client: \n"
                    + ax + "\nexiting");
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
        String pattern = "Server\\d+\\s+(.*)\\s+(\\d+)\\s+(\\d+)$";
        String line;
        Matcher m;

        if (!f.exists() || f.isDirectory()) {
            System.out.println("No se ha encontrado archivo de info de servidores");
            System.exit(1);
        }

        nServers = 0;
        Pattern pat = Pattern.compile(pattern);

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            while ((line = br.readLine()) != null) {

                m = pat.matcher(line);
                if(m.find()) {
                    serverList[nServers] = InetAddress.getByName(m.group(1));
                    serverPorts[nServers] = Integer.parseInt(m.group(2));
                    serverBW[nServers] = Integer.parseInt(m.group(3));
                    nServers ++;
                } else {
                    System.out.println("Linea sin informacion encontrada");
                }
            }

            if (nServers == 0) {
                System.out.println("No servers found, exiting");
                System.exit(1);
            }

        } catch (UnknownHostException uhx) {
            System.out.println("Error parsing server name: " + nServers);
            System.out.println(uhx);
        } catch (IOException iox) {
            System.out.println("Error reading server data file");
            System.out.println(iox);
        }

    }

    private boolean sendHello(InetAddress rHost, int rPort) {
        /**
         * function that acts both as a HELLO protocol and connection test
         * returns true if server is up, false if remote host times out
         */

        String receive;
        try {
            FTPService.sendUDPmessage(s, FTPService.Command.HELLO.toString(), rHost, rPort);
            s.receive(packet);
            receive = new String(packet.getData(), 0, packet.getLength());
            handleResponse(receive, packet.getAddress());

        } catch (IOException gx) {
            System.out.println("server " + rHost.getHostName() + ":" +
             " appears to be down or not responding");
            System.out.println(gx);
            return false;
        }
        return true;
    }

    private void handleCommand() {
        //TODO: handle given command and await coherent response
    }

    private void handleResponse(String sRX, InetAddress rHost) throws IOException{
        //TODO: reformat to work with new server responses.
        FTPService.Response rRX = FTPService.responseFromString(sRX);
        //parseamos respuesta para obtener 'response'
        String output;

        switch (rRX) {

            case WCOME:
                output = ("Welcome to the UDP FTP Server...");
                break;

            case PORT:
                int port = FTPService.portFromResponse(sRX); //obtenemos puerto
                System.out.println("PORT: " + port);
                //clientTCPHandler(rHost, port); //iniciamos cliente TCP

                s.receive(packet); //recibimos transfer OK
                output = new String(packet.getData(), 0, packet.getLength());
                if (FTPService.responseFromString(output).equals(FTPService.Response.OK)) {
                    output = "Transfer OK";
                } else { //Si no contiene el OK ??QUE HACER???
                    output = "Response different from OK";//***********
                }
                packet.setLength(FTPService.SIZEMAX);

                break;

            case BYE:
                output = ("BYE");
                break;

            case SERVERROR: //Lo mandan cuando el archivo no existe o es carpeta
                output = ("SERVER ERROR: File does not exist or is a directory");
                break;

            default: //Incluye unknown y OK. El OK lo recibiriamos desde el TCPhandler
                output = ("UNKNOWN or out of order command");
                break;
        }

        System.out.println(output);
    }

    /*private void clientTCPHandler(InetAddress rHost, int rPort) throws IOException {

        Socket stream = new Socket(rHost, rPort); // conecta  servidor remoto en puerto remoto
        System.out.println("Conexion TCP establecida con " + rHost.toString() + ":" + rPort);

        if (com.equals(FTPService.Command.GET)){
            getTCP(stream, fileName);
        } else if (com.equals(FTPService.Command.LIST)) {
            listTCP(stream);
        }

        System.out.println("Conexion TCP cerrada");
        stream.close();

    }
    */

    private void listTCP(Socket stream) throws IOException{

        String inLine;
        BufferedReader in = new BufferedReader(new InputStreamReader(stream.getInputStream()));
        System.out.println("\nFilename\tBytes");
        do  {//leemos información como caracteres linea a liena
            inLine = in.readLine();
            if (inLine != null) {
                System.out.println(inLine);
            }
        } while (inLine != null);
        System.out.print("\n");
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
}