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

    private FTPService.Command com; // ultimo comando enviado
    private String fileName = ""; //nombre del archivo que pedimos

    private DatagramSocket s;
    private DatagramPacket packet;

    private int nServers;
    private InetAddress serverList[];
    private int serverPorts[];
    private int serverBW[];

    public static void main(String[] args) {

        new UDPFTPClient();
        //TODO: separate code in constructor into 2 parts. the part run in main method and the constructor
    }

    public UDPFTPClient() {
        Scanner input = new Scanner(System.in);

        byte[] buf = new byte[FTPService.SIZEMAX]; //almacena paquete UDP en rx
        String sRX = ""; //mensaje que recibimos
        String sTX; //mensaje que enviamos

        boolean cGet = false; //marca si estamos en rutina de recepcion archivo

        int rPort; //remote UDP port
        int rPortTCP; //remote TCP port
        InetAddress rHost; //remote IP
        String arg_rHost = "10.1.59.102";

        try {
            rHost = InetAddress.getByName(arg_rHost);
            rPort = FTPService.SERVERPORT;

            s = new DatagramSocket(); //SO elige puerto
            s.setSoTimeout(FTPService.TIMEOUT); //timeout 1 segundos
            packet = new DatagramPacket(buf, buf.length);

            sendHello(rHost, rPort); // ejecuta protocolo HELLO

            while (! sRX.equals( FTPService.Response.BYE.toString())) {

                System.out.print("\n>");
                sTX = input.nextLine();
                com = FTPService.commandFromString(sTX); //cogemos comando

                if (com.equals(FTPService.Command.GET)) {
                    fileName = FTPService.requestedFile(sTX); //cogemos archivo pedido
                    File f = new File(fileName);
                    if (f.exists()){
                        System.out.print("file " + fileName + " exists, do you want to overwrite it?\n>");
                        if (! input.nextLine().equals("YES")){
                            continue;
                        }
                    }
                }

                FTPService.sendUDPmessage(s, sTX, rHost, rPort);//Hacemos peticion

                s.receive(packet);
                sRX = new String(packet.getData(),0, packet.getLength());
                packet.setLength(buf.length);

                parseResponse(sRX, packet.getAddress());
                //obtenemos respuesta como string y la interpretamos
            }

            System.out.println("Cerrando sesion FTP ...");
            s.close();

        } catch (Exception ax) {
            System.out.println("Exception caught while running ftp.client: \n"
                    + ax + "\nexiting");
            System.exit(2);
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

    private void sendHello(InetAddress rHost, int rPort) throws Exception{

        String receive;

        FTPService.sendUDPmessage(s, FTPService.Command.HELLO.toString(), rHost, rPort);
        s.receive(packet);
        receive = new String(packet.getData(),0, packet.getLength());
        parseResponse(receive, packet.getAddress());

    }

    private void parseResponse(String sRX, InetAddress rHost) throws Exception{

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
                clientTCPHandler(rHost, port); //iniciamos cliente TCP

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

    private void clientTCPHandler(InetAddress rHost, int rPort) throws Exception {

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

    private static void listTCP(Socket stream) throws Exception{

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

    private static void getTCP(Socket stream, String fileName) throws Exception {

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

    private static void getChunks( ) { }

}