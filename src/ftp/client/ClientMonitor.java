package ftp.client;

import ftp.client.Session.RemoteFile;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Alberto Mur & Javier Antoran
 */
abstract class ClientMonitor {

    private static HashMap<String, RemoteFile> mergedList = new HashMap<String, RemoteFile>();

    public static synchronized HashMap<String, RemoteFile> getMergedList() {
        return mergedList;
    }

    public static synchronized void resetList(){
        ClientMonitor.mergedList = new HashMap<String, RemoteFile>();
    }

    protected static synchronized void writeList(ArrayList<RemoteFile> list) {
        int i, n;

        for(RemoteFile rFile: list) {
            if (!mergedList.containsKey(rFile.getFileName())) {
                mergedList.put(rFile.getFileName(), rFile);
            }
        }

    }

    protected static synchronized void writeFile() { } //unused for now






}
