package ftp.client;

import java.net.InetAddress;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientFileHandler extends ClientTCPHandler{

    public ClientFileHandler(int lPort, InetAddress rHost, int rPort) {
        super(lPort, rHost, rPort);
    }

    @Override
    public void run() {

    }
}
