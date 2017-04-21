package ftp.client;

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
        int i, n;

        for(RemoteFile rFile: list) {
            if (!mergedList.containsKey(rFile.getFileName())) {
                mergedList.put(rFile.getFileName(), rFile);
            }
        }

    }

    protected static synchronized void writeFile() { } //unused for now






}
