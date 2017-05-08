package ftp.client.tcp;

import ftp.FTPService;
import ftp.client.Session.RemoteServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 *  Alberto Mur & Javier Antoran.
 */
abstract public class ClientTCPHandler implements Runnable{


    protected RemoteServer rs;
    protected InetAddress rHost;
    protected int rPort;
    protected int lPort;

    protected Socket stream;

    public ClientTCPHandler(int lPort, RemoteServer rs, int rPort) {
        this.rs = rs;
        this.rPort = rPort;
        this.lPort = lPort;

    }

    public void setRemotePort(int remotePort) {
        this.rPort = remotePort;
    }

    public void setListenPort(int listenPort) {
        this.lPort = listenPort;
    }

    protected void establishTCP() {

        try {

            this.stream = new Socket(this.rs.getAddr(), this.rPort, null, lPort);
            this.stream.setSoTimeout(FTPService.TIMEOUT);

        } catch (IOException e) {
            FTPService.logErr(e.getMessage());
            FTPService.logDebug(e);
        }

    }

}
