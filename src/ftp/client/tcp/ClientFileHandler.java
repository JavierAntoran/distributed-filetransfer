package ftp.client.tcp;

import ftp.FTPService;
import ftp.client.Session.RemoteServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientFileHandler extends ClientTCPHandler{

    private int id;

    private File f;
    private int firstChunk;
    private int lastChunk;
    private int lastReceivedChunk;

    private FileChannel fc;


    public ClientFileHandler(int lPort, RemoteServer rs, int rPort, int id, File f, int firstChunk, int lastChunk) {
        super(lPort, rs, rPort);
        this.id = id;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;
        this.f = f;

    }

    private long getChunk(int nChunk) throws IOException {

        byte buff[] = new byte[FTPService.CHUNKSIZE];
        ByteBuffer byteBuffer;
        InputStream chunkStream = this.stream.getInputStream();
        int dataLength;
        long dataRead = 0;
        long startTime = System.nanoTime();

        this.fc.position((nChunk-1)*FTPService.CHUNKSIZE);

        while ((dataLength = chunkStream.read(buff)) != -1) {
            byteBuffer = ByteBuffer.wrap(buff,0,dataLength );
            while(byteBuffer.hasRemaining()) {
                this.fc.write(byteBuffer);
            }
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
        int nChunks = this.lastChunk - this.firstChunk + 1;
        long bw;
        long avg_bw = 0;
        int bwDivider = 0;

        try {



            fc = FileChannel.open(f.toPath(), WRITE);

            for (i = 0; i < nChunks; i++) {

                establishTCP();
                bw = getChunk(i + this.firstChunk);

                if (bw != -1) {
                    avg_bw += bw;
                    bwDivider++;
                }

                this.stream.close();
                this.lastReceivedChunk = this.firstChunk + i;
            }

            if (FTPService.UPDATE_BW_ON_GET && (avg_bw != 0)){
                FTPService.logInfo(
                        String.format("%s bandwidth updated from %f to: %f",
                                this.rs.getName(),
                                ((float)this.rs.getBw()/1e6),
                                ((float) avg_bw / (bwDivider*1e6)))
                );
                this.rs.setBw(avg_bw / bwDivider);
            }


        } catch (IOException  e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
        } 
    }

    public int getFirstChunk() {
        return firstChunk;
    }

    public int getLastChunk() {
        return lastChunk;
    }

    public int getLastReceivedChunk() {
        return lastReceivedChunk;
    }
}
