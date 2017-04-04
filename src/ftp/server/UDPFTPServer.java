package ftp.server;

import ftp.FTPService;

import java.io.File;
import java.net.*;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class UDPFTPServer {

    static private String ftpPath = "ftp/";

    public static void main(String[] args) {

        byte[] buf = new byte[FTPService.SIZEMAX];
        String sRX;
        String sTX;

        int rPort;
        InetAddress rHost;
        //InetAddress lHost;
        String lHostString = "localhost";// Cambiar esto en version final
        int lPort = FTPService.SERVERPORT;
        int lPortTCP;

        DatagramSocket s;
        DatagramPacket packet;

        try{

            //lHost  = InetAddress.getByName(lHostString);
            s = new DatagramSocket(FTPService.SERVERPORT); //Este escucha
            packet = new DatagramPacket(buf, buf.length);
            System.out.println("Server is listening on " + lPort);

            while (true) {

                s.receive(packet);
                rPort = packet.getPort();
                rHost = packet.getAddress(); //adquirimos direcciony puerto de peticion
                sRX = new String(packet.getData(),0, packet.getLength());
                System.out.println("Received " + rHost.getHostAddress() +
                                ":" + rPort + " " + sRX);

                sTX = parseCommand(sRX, rPort);

                FTPService.sendUDPmessage(s, sTX, rHost, rPort);
                System.out.println("Responded with: " + sTX);

                packet.setLength(buf.length);
            }

        } catch (Exception bx) {
            System.out.println("Exception caught while running ftp.server: \n"
                + bx + "\nexiting");
            System.exit(1);
        }
    }

    static String parseCommand(String sRX, int rPort) {

        FTPService.Command cRX = FTPService.commandFromString(sRX);
        String sTX;

        switch(cRX){
            case HELLO:
                sTX =  FTPService.Response.WCOME.toString();
                break;

            case LIST:
                HandleFTPConnection tcpList = new HandleFTPConnection(0, rPort, ftpPath); //OS elige puerto
                int lPortTCPList = tcpList.getlPort(); //obetemos puerto asignado
                new Thread(tcpList).start(); //lanzamos hilo
                sTX =  (FTPService.Response.PORT.toString() + " " + lPortTCPList); //avisamos cliente
                break;

            case GET:
                String fName = ftpPath + FTPService.requestedFile(sRX); //obtenemos nombre fichero
                File f = new File(fName);//obtenemos fichero

                if(f.exists() && !f.isDirectory()) { //miramos si existe

                    HandleFTPConnection tcpGet = new HandleFTPConnection(0, rPort, f);//OS elige puerto
                    int lPortTCPGet = tcpGet.getlPort(); //obtenemos puerto
                    new Thread(tcpGet).start(); //lanzamos hilo
                    sTX = (FTPService.Response.PORT.toString() + " " + lPortTCPGet); //avisamos cliente

                } else { //Si no existe devolvemos error
                    System.out.println(fName + ": File does not exist or is a directory");
                    sTX = FTPService.Response.SERVERROR.toString(); // si no existe devolvemos error**?
                }
               break;

            case QUIT:
                sTX =  FTPService.Response.BYE.toString();
                break;

            default:
                sTX =  FTPService.Response.UNKNOWN.toString(); //Responder con esto o con SERVERROR?
                break;
        }

        return  sTX;
    }
}
