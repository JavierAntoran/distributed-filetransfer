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


    public ClientFileHandler(int lPort, RemoteServer rs, int rPort, int id, File f, int firstChunk, int lastChunk) {
        super(lPort, rs.getAddr(), rPort);
        this.id = id;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;
        this.f = f;

        this.rs = rs;

    }

    private long getFile() throws IOException {

        byte buff[] = new byte[10000000];
        InputStream dataStream = stream.getInputStream();
        FileOutputStream fOut = new FileOutputStream(f);

        int dataLength;
        long totalData = 0;
        long startTime = System.nanoTime();
        while ((dataLength = dataStream.read(buff, 0, buff.length)) != -1) {
            fOut.write(buff, 0, dataLength);
            totalData += dataLength;
        }
        long endTime = System.nanoTime();
        long timeLapsed = endTime - startTime;
        long bw = (totalData * 1000000000 / timeLapsed);

        fOut.flush();

        fOut.close();
        dataStream.close();
        return bw;
    }

    private long bwCheck() throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        InputStream dataStream = stream.getInputStream();
        long dataLength;
        long dataRead;

        long startTime = System.nanoTime();

        while ((dataLength = dataStream.read(buff)) != -1) {
            dataRead += dataLength;
        }

        if (dataRead != FTPService.BWCHECKSIZE * FTPService.CHUNKSIZE) {
            System.out.println("error in number of bytes read");
            //TODO: do something

        } else {
            long endTime = System.nanoTime();

            long timeLapsed = endTime - startTime;
            long bw = (dataRead * 1000000000 / timeLapsed);

            System.out.println(this.addr.getHostName() +
                    ": bandwidth updated to: " +  ((float) bw / 1000000));

        return 0;
    }

    private long getChunk() throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        InputStream dataStream = stream.getInputStream();
        FileOutputStream fOut = new FileOutputStream(f);
        int dataLength;

        while ((dataLength = dataStream.read(buff)) != -1) {
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
                getChunk();

            this.stream.close();

        } catch (IOException benix){
            System.out.println(benix.getMessage());
            //TODO: implement resending request if servers go down
        } finally {

            if (FTPService.UPDATE_BW_ON_GET){
                rs.setBw(this.bw);
            }
        }
    }


}
