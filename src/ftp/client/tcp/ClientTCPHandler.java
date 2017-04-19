package ftp.client.tcp;

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

    public void setRemoteHost(InetAddress rHost) {
        this.rHost = rHost;
    }

    public void setRemotePort(int remotePort) {
        this.rPort = remotePort;
    }

    public void setListenPort(int listenPort) {
        this.lPort = listenPort;
    }

    protected void establishTCP() {

        try {

            this.stream = new Socket(this.rHost, this.rPort, null, lPort);

        } catch (IOException iox) {
            System.out.println(iox);
        }

    }

}
