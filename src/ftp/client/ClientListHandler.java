package ftp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientListHandler extends ClientTCPHandler {

    protected ArrayList<String> fileList = new ArrayList<String>();
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

        try {

            establishTCP();
            String inLine;
            BufferedReader in = new BufferedReader(new InputStreamReader(stream.getInputStream()));

            do {//leemos información como caracteres linea a liena
                inLine = in.readLine();
                if (inLine != null) {
                    this.fileList.add(inLine);
                }
            } while (inLine != null);

            ClientMonitor.writeList(this.fileList);

            in.close();
            this.stream.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            //TODO: handle exception if server goes down mid stream by updating serverlist
        }

    }
}
