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

    private FileOutputStream fOut;


    public ClientFileHandler(int lPort, RemoteServer rs, int rPort, int id, File f, int firstChunk, int lastChunk) {
        super(lPort, rs, rPort);
        this.id = id;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;
        this.f = f;

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

    private long getChunk(int nChunk) throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        InputStream chunkStream = this.stream.getInputStream();
        int dataLength;
        long dataRead = 0;
        long startTime = System.nanoTime();

        this.fOut.getChannel().position((nChunk)*FTPService.CHUNKSIZE);

        while ((dataLength = chunkStream.read(buff)) != -1) {
            this.fOut.write(buff, 0, dataLength);
            dataRead += dataLength;
        }
        long endTime = System.nanoTime();

        long timeLapsed = endTime - startTime;
        long bw = (dataRead * 1000000000 / timeLapsed);

        if (dataRead != FTPService.CHUNKSIZE) {
            bw = -1;
        }


        chunkStream.close();

        return bw;
    }

    @Override
    public void run() {

        int i;
        int nChunks = this.lastChunk - this.firstChunk;
        long bw;
        long avg_bw = 0;

        try {

            this.fOut = new FileOutputStream(f);

            for (i = 0; i < nChunks; i++) {

                establishTCP();
                bw = getChunk(i + this.firstChunk);

                if (bw != -1) {
                    avg_bw += bw;
                }

                this.stream.close();
                fOut.flush();
            }
            fOut.close();

            if (FTPService.UPDATE_BW_ON_GET && (avg_bw != 0)){
                rs.setBw(avg_bw /= (nChunks - 1));
            }


        } catch (IOException  e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
            //TODO: implement resending request if servers go down
        } 
    }


}
