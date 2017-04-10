package ftp.server;

import ftp.FTPService;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created by StFrancisco on 05/04/2017.
 */
public class HandleChunk extends HandleFTPConnection {

    private File f;
    private FileInputStream fis;

    private int firstChunk;
    private int lastChunk;
    private byte[] chunkBytes;

    public HandleChunk(File f, int firstChunk, int lastChunk, int lPort,
                       InetAddress rHost, int rPort) throws Exception {

        super(lPort, rHost, rPort);

        this.f = f;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;

        this.fis = new FileInputStream(f);
    }

    private int getChunk(int nChunk, int chunkSize) throws Exception {

        this.chunkBytes = new byte[chunkSize];
        int dataread;
        System.out.printf("Reading chunk %d\n", nChunk);
        //TODO: check if amount of bytes read is correct for last chunk
        fis.getChannel().position((nChunk-1)*FTPService.CHUNKSIZE);
        if ( (dataread = fis.read(chunkBytes) ) <= 0) {
            System.out.println("error leyendo chunkBytes: " + nChunk);
        }

        return dataread;
    }

    private void sendChunk(int nChunk) throws Exception {
        sendChunk(nChunk, nChunk);
    }

    private void sendChunk(int firstChunk, int lastChunk) throws Exception {

        int i;
        String msg;

        // Sets read pointer position

        for (i = firstChunk; i <= lastChunk; i++) {

            int chunkSize = getChunk(i, FTPService.CHUNKSIZE);
            System.out.println(chunkSize);
            this.out.write(this.chunkBytes, 0, chunkSize);
            System.out.println("file: " + this.f.getPath() + " chunk sent: " + i);


        }
        msg = " chunk " + firstChunk + "-" + lastChunk;
        sendUDPOK(msg);


    }

    private void sendFile() throws Exception{

        System.out.println("Sending file: " + f.getPath());

        byte buffer[] = new byte[10000000];

        int dataLength;
        while ((dataLength = this.fis.read(buffer)) > 0) {
            this.out.write(buffer, 0, dataLength);
        }
        System.out.println("file: " + f.getPath() + "sent");
        sendUDPOK(null);
    }

    public void run() {

        System.out.println("FTP chunk handler launched." );
        try {

            establishTCP();

            if (firstChunk != 0) {
                sendChunk(firstChunk, lastChunk);
            } else {
                sendFile();
            }

            this.out.close();

            this.clientSocket.close();
            this.welcomingSocket.close();

        } catch (Exception fx) {}
    }


}
