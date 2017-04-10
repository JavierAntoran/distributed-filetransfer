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

/*private void clientTCPHandler(InetAddress rHost, int rPort) throws IOException {

        Socket stream = new Socket(rHost, rPort); // conecta  servidor remoto en puerto remoto
        System.out.println("Conexion TCP establecida con " + rHost.toString() + ":" + rPort);

        if (com.equals(FTPService.Command.GET)){
            getTCP(stream, fileName);
        } else if (com.equals(FTPService.Command.LIST)) {
            listTCP(stream);
        }

        System.out.println("Conexion TCP cerrada");
        stream.close();

    }
    */