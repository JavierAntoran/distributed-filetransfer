package ftp.server;

import ftp.FTPService;

import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created by StFrancisco on 05/04/2017.
 */
public class HandleChunk extends HandleFTPConnection {

    private File f;

    private int firstChunk;
    private int lastChunk;
    private byte[] chunkBytes;

    public HandleChunk(File f, int lPort, InetAddress rHost, int rPort) {

        super(lPort, rHost, rPort);
        this.f = f;
    }

    private int getChunk(int nChunk, int chunkSize) throws Exception {

        this.chunkBytes = new byte[chunkSize];
        int dataread;

        if ( (dataread = fis.read(chunkBytes)) <= 0) {
            System.out.println("error leyendo chunkBytes: " + nChunk);
        }

        return dataread;
    }

    private void sendChunk(int nChunk) throws Exception {

        int chunkSize = getChunk(nChunk, FTPService.CHUNKSIZE);
        this.out.write(this.chunkBytes, 0, chunkSize);
        System.out.println("file: " + this.f.getPath() + " chunkBytes sent: " + nChunk);
        sendUDPOK(this.rHost, this.rPortUDP, "chunk: " + nChunk);
    } //when sending one chunk

    private void sendChunk(int firstChunk, int lastChunk) throws Exception {

        int i;
        for (i = firstChunk; i <= lastChunk; i++) {

            int chunkSize = getChunk(i, FTPService.CHUNKSIZE);
            this.out.write(this.chunkBytes, 0, chunkSize);
            System.out.println("file: " + this.f.getPath() + " chunk sent: " + i);
            sendUDPOK(this.rHost, this.rPortUDP, "chunk: " + i);
        }

    }//when sending chunk interval

    private void sendFile(File f) throws Exception{

        System.out.println("Sending file: " + f.getPath());

        byte buffer[] = new byte[10000000];

        int dataLength;
        while ((dataLength = this.fis.read(buffer)) > 0) {
            this.out.write(buffer, 0, dataLength);
        }
        System.out.println("data sent");
    }
    @Override
    public void run() {

        System.out.println("FTP Listing handler launched." );
        try {

            this.welcomingSocket = new ServerSocket(this.lPort);
            this.welcomingSocket.setSoTimeout(this.soTimeOut);
            this.dSocket = new DatagramSocket();

            sendPortCommand(this.welcomingSocket.getLocalPort());

            this.clientSocket = this.welcomingSocket.accept();

            this.out = this.clientSocket.getOutputStream();


        } catch (Exception fx) {}
    }


}
