package ftp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientListHandler extends ClientTCPHandler {

    public ClientListHandler(int lPort, InetAddress rHost, int rPort) {
        super(lPort, rHost, rPort);

    }

    @Override
    public void run() {

        try {

            String inLine;
            BufferedReader in = new BufferedReader(new InputStreamReader(stream.getInputStream()));

            do {//leemos información como caracteres linea a liena
                inLine = in.readLine();
                if (inLine != null) {
                    System.out.println(inLine);
                }
            } while (inLine != null);
            System.out.print("\n");

        } catch (IOException iox) {
            System.out.println(iox);
        }

    }
}
