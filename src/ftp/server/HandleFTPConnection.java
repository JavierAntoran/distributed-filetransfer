package ftp.server;


import ftp.FTPService;

import java.io.*;
import java.net.*;

/**
 * Alberto Mur & Javier Antoran
 */
abstract public class HandleFTPConnection implements Runnable {

    protected int soTimeOut = FTPService.TIMEOUT;

    protected Socket clientSocket;
    protected ServerSocket welcomingSocket;
    protected DatagramSocket dSocket;

    protected int lPort;
    protected InetAddress rHost;
    protected int rPort;

    protected OutputStream out;

    public HandleFTPConnection(int lPort, InetAddress rHost, int rPort){
        this.lPort = lPort;
        this.rPort = rPort;
        this.rHost = rHost;
    }

    protected void sendPortCommand(int port) throws IOException{

        String msg = FTPService.Response.PORT.toString();
        msg += " " + port;

        FTPService.sendUDPmessage(this.dSocket, msg, this.rHost, this.rPort);
        System.out.println("> " + msg);

    }

    protected void sendUDPOK(String msg) throws IOException {
        if (msg != null) {
            msg = FTPService.Response.OK.toString() + msg;
        } else {
            msg = FTPService.Response.OK.toString();
        }

        FTPService.sendUDPmessage(this.dSocket, msg, this.rHost, this.rPort);
        System.out.println("> " + msg);
    }

}
