package ftp.client;

import ftp.client.Session.RemoteFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientListHandler extends ClientTCPHandler {

    protected ArrayList<RemoteFile> fileList = new ArrayList<RemoteFile>();
    private int id;

    public ClientListHandler(int lPort, InetAddress rHost, int rPort, int id) {
        super(lPort, rHost, rPort);
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public void run() {
        Pattern p;
        Matcher m;
        ArrayList<String> rawList = new ArrayList<String>();

        try {

            establishTCP();
            String inLine;
            BufferedReader in = new BufferedReader(new InputStreamReader(stream.getInputStream()));

            do {//leemos información como caracteres linea a liena
                inLine = in.readLine();
                if (inLine != null) {
                    rawList.add(inLine);
                }
            } while (inLine != null);

            in.close();
            this.stream.close();

            p = Pattern.compile("(.*)\\t(\\d+)$");

            for(String rawFile: rawList) {
                m = p.matcher(rawFile);
                if (m.find()) {
                    RemoteFile rFile = new RemoteFile();
                    rFile.setFileName(m.group(1));
                    rFile.setFileSize(Integer.parseInt(m.group(2)));
                    this.fileList.add(rFile);
                }
            }

            ClientMonitor.writeList(this.fileList);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            //TODO: handle exception if server goes down mid stream by updating serverlist
        }

    }
}
