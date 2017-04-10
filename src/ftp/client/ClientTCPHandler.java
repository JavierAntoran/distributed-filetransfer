package ftp.client;

import java.net.InetAddress;

/**
 *  Alberto Mur & Javier Antoran.
 */
public class ClientTCPHandler {

    public ClientTCPHandler(InetAddress rHost, int rPort) {



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