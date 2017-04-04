package ftp.client;

import ftp.FTPService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class UDPFTPClient {

    static FTPService.Command com; // ultimo comando enviado
    static String fileName = ""; //nombre del archivo que pedimos

    static DatagramSocket s;
    static DatagramPacket packet;

    public static void main(String[] args) {

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

    static void sendHello(InetAddress rHost, int rPort) throws Exception{

        String receive;

        FTPService.sendUDPmessage(s, FTPService.Command.HELLO.toString(), rHost, rPort);
        s.receive(packet);
        receive = new String(packet.getData(),0, packet.getLength());
        parseResponse(receive, packet.getAddress());

    }

    static void parseResponse(String sRX, InetAddress rHost) throws Exception{

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

     static private void clientTCPHandler(InetAddress rHost, int rPort) throws Exception {

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

}