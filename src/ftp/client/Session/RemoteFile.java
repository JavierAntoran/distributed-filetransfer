package ftp.client.Session;

import ftp.FTPService;

import java.util.ArrayList;

/**
 * Created by StFrancisco on 10/04/2017.
 */
public class RemoteFile {

    private String fileName;
    private long fileSize;
    private long nChunks;

    public RemoteFile(){}

    public RemoteFile(String fileName, long fileSize) {

        this.fileName = fileName;
        this.fileSize = fileSize;
        this.nChunks = FTPService.getNChunks(this.fileSize, FTPService.CHUNKSIZE);

    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getnChunks() {
        return nChunks;
    }

    public String[] getIntervals(ArrayList<RemoteServer> servers) {
        int i;
        int nServers = servers.size();
        String interval[] = new String[nServers];
        int totalBW = 0;
        int relativeBW = 0;
        int chunkOffset = 0;
        long nChunks = FTPService.getNChunks(fileSize, FTPService.CHUNKSIZE);

        for (i = 0; i < nServers; i++) {
            totalBW += servers.get(i).getBw();
        }

        for (i = 0; i < nServers; i++) {
            relativeBW = Math.round(nChunks * servers.get(i).getBw() / totalBW);
            interval[i] = ".part" + (chunkOffset + 1) + "-" + (relativeBW + chunkOffset);
            chunkOffset += relativeBW;
        }

        if (chunkOffset - nChunks > 0) {
            interval[nServers - 1] = ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset - 1);
        } else if (chunkOffset - nChunks > 0) {
            interval[nServers - 1] = ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset + 1);
        }

        return interval;
    }

}
