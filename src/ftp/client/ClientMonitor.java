package ftp.client;

import java.util.ArrayList;

/**
 * Alberto Mur & Javier Antoran
 */
abstract class ClientMonitor {

    private static ArrayList<String> mergedList = new ArrayList<String>();

    public static ArrayList<String> getMergedList() {
        return mergedList;
    }

    public static void resetList(){
        mergedList = new ArrayList<String>();
    }

    protected static synchronized void writeList(ArrayList<String> list) {
        int i, n;
        boolean exists;

        for (i = 0; i < list.size(); i++) {
            exists = false;
            for(n = 0; n < mergedList.size(); n++) {
                if (mergedList.get(n).equals(list.get(i))) {
                    exists = true;
                }
            }
            if (!exists) {
                mergedList.add(list.get(i));
            }
        }

    }






}
