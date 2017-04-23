package ftp.client;

import ftp.FTPService;
import ftp.client.Session.RemoteFile;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Alberto Mur & Javier Antoran
 */
abstract public class ClientMonitor {

    private static HashMap<String, RemoteFile> mergedList = new HashMap<String, RemoteFile>();

    public static synchronized HashMap<String, RemoteFile> getMergedList() {
        return mergedList;
    }

    public static synchronized void resetList(){
        // TODO: check if there is a better way to do this.
        ClientMonitor.mergedList = new HashMap<String, RemoteFile>();
    }

    public static synchronized void writeList(ArrayList<RemoteFile> list) {
        RemoteFile rf;
        for(RemoteFile rFile: list) {
            if (!mergedList.containsKey(rFile.getFileName())) {
                mergedList.put(rFile.getFileName(), rFile);
            } else {
                rf = mergedList.get(rFile.getFileName());
                if (rFile.getFileSize() == rf.getFileSize()) {
                    mergedList.get(rFile.getFileName()).addServer(rFile.getServerList().get(0));
                } else {
                    FTPService.logWarn(
                            String.format("Filename %s detected on %s with " +
                                    "different size",
                                    rFile.getFileName(),
                                    rFile.getServerList().get(0))
                    );
                }
            }
        }

    }

}
