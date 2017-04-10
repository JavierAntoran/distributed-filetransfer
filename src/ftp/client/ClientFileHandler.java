package ftp.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientFileHandler extends ClientTCPHandler{

    private int id;

    private File f;
    private int firstChunk;
    private int lastChunk;

    private boolean chunkMode = true;

    public ClientFileHandler(int lPort, InetAddress rHost, int rPort, int id, File f, int firstChunk, int lastChunk) {
        super(lPort, rHost, rPort);
        this.id = id;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;
        this.f = f;

        if (this.firstChunk == 0) {
            this.chunkMode = false;
        }
    }

    private byte[] getChunk() {

        return null;
    }

    private void getFile() throws IOException {

        byte buff[] = new byte[10000000];
        InputStream dataStream = stream.getInputStream();
        FileOutputStream fOut = new FileOutputStream(f);

        int dataLength;
        while ((dataLength = dataStream.read(buff, 0, buff.length)) != -1) {
            fOut.write(buff, 0, dataLength);
        }
        fOut.flush();

        fOut.close();
        dataStream.close();

    }




    @Override
    public void run() {

        establishTCP();

        try {

            if (!chunkMode) {
                getFile();
            }

            this.stream.close();

        } catch (IOException benix){
            System.out.println(benix.getMessage());
            //TODO: implement resending request if servers go down
        }
    }



}
