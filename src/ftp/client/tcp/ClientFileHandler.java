package ftp.client.tcp;

import ftp.FTPService;
import ftp.client.Session.RemoteServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientFileHandler extends ClientTCPHandler{

    private int id;

    private File f;
    private int firstChunk;
    private int lastChunk;
    private  RemoteServer rs;

    private InputStream fileStream;


    public ClientFileHandler(int lPort, RemoteServer rs, int rPort, int id, File f, int firstChunk, int lastChunk) {
        super(lPort, rs.getAddr(), rPort);
        this.id = id;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;
        this.f = f;

        this.rs = rs;

    }


    private long bwCheck() throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        InputStream dataStream = stream.getInputStream();
        long dataLength;
        long dataRead = 0;

        long startTime = System.nanoTime();

        while ((dataLength = dataStream.read(buff)) != -1) {
            dataRead += dataLength;
        }



        long endTime = System.nanoTime();

        long timeLapsed = endTime - startTime;
        long bw = (dataRead * 1000000000 / timeLapsed);

        System.out.println(this.rHost.getHostName() +
                ": bandwidth updated to: " +  ((float) bw / 1000000));

        return 0;
    }

    private long getChunk() throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        FileOutputStream fOut = new FileOutputStream(f);
        int dataLength;
        long dataRead = 0;
        long startTime = System.nanoTime();

        while ((dataLength = this.fileStream.read(buff)) != -1) {
            fOut.write(buff, 0, dataLength);
            dataRead += dataLength;
        }
        long endTime = System.nanoTime();

        long timeLapsed = endTime - startTime;
        long bw = (dataRead * 1000000000 / timeLapsed);
        fOut.flush();
        fOut.close();

        return bw;
    }




    @Override
    public void run() {

        int i;
        long bw = 0;

        try {
            fileStream = this.stream.getInputStream();

            for (i = 0; i < this.lastChunk - this.firstChunk; i++) {

                establishTCP();
                bw = getChunk();
                this.stream.close();
            }
            this.fileStream.close();

        } catch (IOException  e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            //TODO: implement resending request if servers go down
        } finally {

            if (FTPService.UPDATE_BW_ON_GET && (bw != 0)){
                rs.setBw(bw);
            }
        }
    }


}
