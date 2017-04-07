package ftp.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Alberto Mur & Javier Antoran.
 */
public class HandleList extends HandleFTPConnection{

    private File f;

    public HandleList(File f, int lPort, InetAddress rHost, int rPort) throws Exception{
        super(lPort, rHost, rPort);
        this.f = f;
    }

    public void run() {

        System.out.println("FTP Listing handler launched." );
        try {

            this.welcomingSocket = new ServerSocket(this.lPort);
            this.welcomingSocket.setSoTimeout(this.soTimeOut);
            this.dSocket = new DatagramSocket();

            sendPortCommand(this.welcomingSocket.getLocalPort());

            this.clientSocket = this.welcomingSocket.accept();

            this.out = this.clientSocket.getOutputStream();

            Path dir = Paths.get(this.f.getAbsolutePath());
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
                String filename;
                for (Path entry : stream) {
                    if (! Files.isDirectory(entry)) {
                        filename = entry.getFileName().toString();
                    } else {
                        filename = "(dir) " + entry.getFileName().toString();
                    }
                    this.out.write(new String(filename +  "\t" + Files.size(entry) + "\n").getBytes());
                }
                this.out.flush();

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            this.out.close();

            this.clientSocket.close();
            this.welcomingSocket.close();

            sendUDPOK(null);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
