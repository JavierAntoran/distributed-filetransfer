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

    private ArrayList<RemoteServer> serverList = new ArrayList<RemoteServer>();

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

    public void addServer(RemoteServer rs) {
        if (! this.serverList.contains(rs) ) {
            this.serverList.add(rs);
        }
    }

    public void removeServer(RemoteServer rs) {
        if (this.serverList.contains(rs)) {
            this.serverList.remove(rs);
        }
    }

    public ArrayList<RemoteServer> getServerList() {
        return this.serverList;
    }

    /**
     * Returns the part extension for each remote server based on
     * available bandwidth
     * ArrayList<RemoteServer> servers
     * ArrayList<String>
     */
    //TODO: switch to better algorithm
    public ArrayList<String> getPartsPerServer(ArrayList<RemoteServer> servers) {

        ArrayList<String> partsPerServer = new ArrayList<String>();
        int totalBW = 0;
        int relativeBW = 0;
        int chunkOffset = 0;
        long nChunks = FTPService.getNChunks(fileSize, FTPService.CHUNKSIZE);

        for (RemoteServer rs: servers) {
            totalBW += rs.getBw();
        }

        for (RemoteServer rs: servers) {
            relativeBW = Math.round(nChunks * rs.getBw() / totalBW);
            partsPerServer.add(".part" + (chunkOffset + 1) + "-" + (relativeBW + chunkOffset));
            chunkOffset += relativeBW;
        }

        if (chunkOffset - nChunks > 0) {
            partsPerServer.set(servers.size()-1, ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset - 1));
        } else if (chunkOffset - nChunks > 0) {
            partsPerServer.set(servers.size()-1, ".part" + (chunkOffset - relativeBW + 1)
                    + "-" + (chunkOffset + 1));
        }

        return partsPerServer;
    }

}
