package ftp.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 *  Alberto Mur & Javier Antoran.
 */
abstract public class ClientTCPHandler implements Runnable{

    protected InetAddress rHost;
    protected int rPort;
    protected int lPort;

    protected Socket stream;

    public ClientTCPHandler(int lPort, InetAddress rHost, int rPort) {

        this.rHost = rHost;
        this.rPort = rPort;
        this.lPort = lPort;

    }

    protected void establishTCP() {

        try {

            this.stream = new Socket(this.rHost, this.rPort, null, lPort);

        } catch (IOException iox) {
            System.out.println(iox);
        }

    }

}
