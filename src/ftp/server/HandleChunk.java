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
        this.fis.getChannel().position((nChunk-1)*FTPService.CHUNKSIZE);
        if ( (dataread = fis.read(chunkBytes) ) <= 0) {
            System.out.println("error leyendo chunkBytes: " + nChunk);
        }

        return dataread;
    }


    private void sendChunk(int nChunk) throws Exception {

        int i;

        int chunkSize = getChunk(nChunk, FTPService.CHUNKSIZE);
        System.out.println(chunkSize);
        this.out.write(this.chunkBytes, 0, chunkSize);
        System.out.println("file: " + this.f.getPath() + " chunk sent: " + nChunk);



    }

    public void run() {

        System.out.println("FTP chunk handler launched." );
        int i;
        String msg;
        try {

            establishTCP();

            for (i = this.firstChunk; i <= this.lastChunk; i++) {
                sendChunk(i);
                reestablishTCP();
            }
            this.out.close();
            this.clientSocket.close();
            this.welcomingSocket.close();
            msg = " chunk " + firstChunk + "-" + lastChunk;
            sendUDPOK(msg);

        } catch (Exception fx) {}
    }


}
