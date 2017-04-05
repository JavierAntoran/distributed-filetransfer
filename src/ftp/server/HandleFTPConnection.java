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
abstract public class HandleFTPConnection implements Runnable{

    protected int soTimeOut = 3000;

    protected Socket clientSocket;
    protected ServerSocket welcomingSocket;
    protected DatagramSocket dSocket;

    protected int lPort;
    protected InetAddress rAddress;
    protected int rPort;

    protected OutputStream out;

    public HandleFTPConnection(int lPort, InetAddress rAddress, int rPort){
        this.lPort = lPort;
        this.rPort = rPort;
        this.rAddress = rAddress;
    }

    protected void sendPortCommand(int port) throws IOException{

        String msg = FTPService.Response.PORT.toString();
        msg += " " + port;

        FTPService.sendUDPmessage(this.dSocket, msg, this.rAddress, this.rPort);
        System.out.println("> " + msg);

    }

    protected void sendUDPOK(String msg) throws IOException {
        if (msg != null) {
            msg = FTPService.Response.OK.toString() + msg;
        } else {
            msg = FTPService.Response.OK.toString();
        }

        FTPService.sendUDPmessage(this.dSocket, msg, this.rAddress, this.rPort);
        System.out.println("> " + msg);
    }

}
