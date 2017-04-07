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

    private void sendList(Path dir) {

        try {
            DirectoryStream<Path> list = Files.newDirectoryStream(dir);
            String filename;
            for (Path entry : list) {
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

    }

    public void run() {

        System.out.println("FTP Listing handler launched." );
        try {

            establishTCP();

            Path dir = Paths.get(this.f.getAbsolutePath());

            sendList(dir);

            this.out.close();

            this.clientSocket.close();
            this.welcomingSocket.close();

            sendUDPOK(null);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
