package ftp.server;

import ftp.FTPService;

import java.io.File;

/**
 * Created by StFrancisco on 05/04/2017.
 */
public class HandleChunk extends HandleFTPConnection {

    private int firstChunk;
    private int lastChunk;
    private byte[] chunkBytes;

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

        System.out.println("FTP handler launched, mode: " );
        try {
            this.toClient = this.welcoming.accept();
            rHost = this.toClient.getInetAddress();
            System.out.println("TCP conection established with " + rHost.toString() + ":" + toClient.getPort());

           /* if (this.toClient.getInetAddress().equals(this.lHost) ) { *****???****
                System.out.println("Conectado a host erroneo");
            }*/

            this.out = this.toClient.getOutputStream();

            if (this.t.equals(task.LIST)) { //averiguamos que accion tomar
                sendList();
            } else if (this.t.equals(task.GETFILE)) {
                sendFile(this.f);
                this.fis.close();
            } else if (this.t.equals(task.GETCHUNKS)) {
                sendChunk(this.firstChunk, this.lastChunk);
                this.fis.close();
            }

            this.out.close();

            this.toClient.close();
            this.welcoming.close();

            System.out.println("TCP Conection with " + rHost.toString() + ":" + toClient.getPort() + " closed");

            sendUDPOK(this.rHost, this.rPortUDP);

        } catch (Exception fx) {}
    }
}
