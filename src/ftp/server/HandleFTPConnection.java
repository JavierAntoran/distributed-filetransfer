package ftp.server;


import ftp.FTPService;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class HandleFTPConnection {

    protected Socket toClient;
    protected ServerSocket welcoming;

    protected InetAddress lHost;
    protected int lPort;
    protected InetAddress rHost;
    protected int rPortUDP;

    protected OutputStream out;
    protected  FileInputStream fis;

    public HandleFTPConnection(int lPort, int rPortUDP) throws Exception {

        this.lPort = lPort;
        this.rPortUDP = rPortUDP;
        this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

        this.lPort = welcoming.getLocalPort();
        this.lHost = welcoming.getInetAddress();

    }

    public int getlPort() {
        return lPort; //devolvemos puerto para RESPONSE: PORT X
    }



    private void sendUDPOK(InetAddress rHost, int rPortUDP) throws Exception {

        String text = FTPService.Response.OK.toString();
        byte[] buff = text.getBytes();

        DatagramSocket d = new DatagramSocket(); // SO elige puerto
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rHost, rPortUDP);
        d.send(packet);
        System.out.println("Sent: " + text);
    }//will be deprecated soon

    private void sendUDPOK(InetAddress rHost, int rPortUDP, String message) throws Exception {
        /*
        use when confirming specific file or action
         */
        String text = FTPService.Response.OK.toString() + " " + message;
        byte[] buff = text.getBytes();

        DatagramSocket d = new DatagramSocket(); // SO elige puerto
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rHost, rPortUDP);
        d.send(packet);
        System.out.println("Sent: " + text);
    }

}
