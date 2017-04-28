package ftp.client.Session;

import ftp.FTPService;

import java.util.ArrayList;

import static java.util.Arrays.sort;

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

    public boolean removeServer(RemoteServer rs) {
        return this.serverList.remove(rs);
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
        long nChunks = FTPService.getNChunks(this.fileSize, FTPService.CHUNKSIZE);

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

    public static int[] distributeChunks(ArrayList<RemoteServer> servers, long fileSize, Integer[] chunks) {

        int partsPerServer[] = new int[servers.size()];
        int totalBW = 0;
        int nChunks = chunks.length;
        int residualChunks = nChunks;
        int chunkBytes = 0;
        float minTime = 1000000; //max transfer time is 11.57 days
        float serverTime;
        int minTimeIndex = 0;
        boolean unevenChunk = false;
        int unevenIndex = servers.size() + 1;
        int nextChunkIndex = nChunks;

        int[] out = new int[2 * servers.size()]; //default initialized to 0

        int i;
        int j;

        sort(chunks);

        for (RemoteServer rs: servers) {
            totalBW += rs.getBw();
        }

        // Get residual chunks from fair distribution
        for (i = 0; i < servers.size(); i++) {
            partsPerServer[i] = (int) (nChunks * servers.get(i).getBw() / totalBW);
            residualChunks -= partsPerServer[i];
        }

        // If there are residual chunks assign them in the most efficient
        // way in terms of global time consumption
        for (j = 0; j < residualChunks; j++) {

            chunkBytes = FTPService.CHUNKSIZE;

            // Check if last chunk is a non-fullsize chunk
            if ((j == residualChunks - 1) && ( fileSize % FTPService.CHUNKSIZE != 0)) {
                chunkBytes = (int) (fileSize % FTPService.CHUNKSIZE);
                unevenChunk = true;
            }

            // Calc minimal time for efficient chunk distribution
            for (i = 0; i < servers.size(); i++) {
                serverTime = (partsPerServer[i] * FTPService.CHUNKSIZE + chunkBytes) / servers.get(i).getBw();
                if ( serverTime < minTime ) {
                    minTime = serverTime;
                    minTimeIndex = i;
                }
            }
            partsPerServer[ minTimeIndex ]++;
        }


        if (unevenChunk) {
            unevenIndex = minTimeIndex;
            out[2 * unevenIndex + 1] = chunks[chunks.length - 1];
            out[2 * unevenIndex] = chunks[chunks.length - partsPerServer[unevenIndex]];
            nextChunkIndex -= partsPerServer[unevenIndex];
        }


        for (i = 0; i < servers.size(); i++) {
            if (i == unevenIndex) {
                continue;
            }
            if (partsPerServer[i] == 0) {
                out[2 * i + 1] = 0;
                out[2 * i] = 0;
            } else {
                out[2 * i + 1] = chunks[nextChunkIndex];
                out[2 * i] = out[2 * i + 1] - partsPerServer[i] + 1;
            }
            nextChunkIndex -= partsPerServer[i];
        }

        return out;
    }

    public static int minIndexFloat(float[] array){

        int minIndex = 0;
        float minValue = 0;

        for (int i = 0; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
                minIndex = i;
            }
        }
            return minIndex;
    }

}
