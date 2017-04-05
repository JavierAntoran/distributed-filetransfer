package ftp.server;

import ftp.FTPService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by StFrancisco on 05/04/2017.
 */
public class HandleList extends HandleFTPConnection{

    public HandleList(File f, int firstChunk, int lastChunk) throws Exception{


    }

    private void sendList() throws Exception{

        BufferedWriter buffw = new BufferedWriter(new OutputStreamWriter(this.out));
        Path dir = Paths.get(ftpPath);
        DirectoryStream<Path> dirList = Files.newDirectoryStream(dir);

        String fileName;

        for (Path p: dirList) {
            if (! Files.isDirectory(p)) {
                fileName = p.getFileName().toString();
            } else {
                fileName = "(dir) " + p.getFileName().toString();
            }
            buffw.write(fileName +  "\t" + Files.size(p) + "\n");
            buffw.flush();
        }
        buffw.close();
        out.close();

    }

    @Override
    public void run() {

        System.out.println("FTP handler launched, mode: " );
        try {
            this.toClient = this.welcoming.accept();
            rHost = this.toClient.getInetAddress();
            System.out.println("TCP conection established with " + rHost.toString() + ":" + toClient.getPort());

           /* if (this.toClient.getInetAddress().equals(this.lHost) ) { *****???****
                System.out.println("Conectado a host erroneo");
            }*/

            this.out = this.toClient.getOutputStream();

            if (this.t.equals(task.LIST)) { //averiguamos que accion tomar
                sendList();
            } else if (this.t.equals(task.GETFILE)) {
                sendFile(this.f);
                this.fis.close();
            } else if (this.t.equals(task.GETCHUNKS)) {
                sendChunk(this.firstChunk, this.lastChunk);
                this.fis.close();
            }

            this.out.close();

            this.toClient.close();
            this.welcoming.close();

            System.out.println("TCP Conection with " + rHost.toString() + ":" + toClient.getPort() + " closed");

            sendUDPOK(this.rHost, this.rPortUDP);

        } catch (Exception fx) {}
    }

}
