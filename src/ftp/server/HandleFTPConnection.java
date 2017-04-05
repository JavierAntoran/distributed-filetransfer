package ftp.server;


import ftp.FTPService;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class HandleFTPConnection implements Runnable{

    public enum task {LIST, GETFILE, GETCHUNKS}; //Esto nos marca si mandamos ls o archivo

    private Socket toClient;
    private ServerSocket welcoming;

    private InetAddress lHost;
    private int lPort;
    private InetAddress rHost;
    private int rPortUDP;

    private OutputStream out;
    private  FileInputStream fis;
    private task t;
    private File f;
    private String ftpPath;

    private int firstChunk;
    private int lastChunk;
    private byte[] chunkBytes;

    public HandleFTPConnection(int lPort, int rPortUDP, String ftpPath) throws Exception {


        this.t = task.LIST;
        this.lPort = lPort;
        this.rPortUDP = rPortUDP;
        this.ftpPath = ftpPath;
        this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

        this.lPort = welcoming.getLocalPort();
        this.lHost = welcoming.getInetAddress();

    }

    public HandleFTPConnection(int lPort, int rPortUDP, File f) throws Exception{

        this.t = task.GETFILE;
        this.lPort = lPort;
        this.rPortUDP = rPortUDP;
        this.f = f;
        this.fis = new FileInputStream(this.f);

        this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

        this.lPort = welcoming.getLocalPort();
        this.lHost = welcoming.getInetAddress();
    }

    public HandleFTPConnection(int lPort, int rPortUDP, File f, int firstChunk, int lastChunk) throws Exception{

        this.t = task.GETCHUNKS;
        this.lPort = lPort;
        this.rPortUDP = rPortUDP;

        this.f = f;
        this.firstChunk = firstChunk;
        this.lastChunk = lastChunk;

        this.fis = new FileInputStream(this.f);

        this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

        this.lPort = welcoming.getLocalPort();
        this.lHost = welcoming.getInetAddress();

    }

    public int getlPort() {
        return lPort; //devolvemos puerto para RESPONSE: PORT X
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

    private int getChunk(int nChunk, int chunkSize) throws Exception {

        this.chunkBytes = new byte[chunkSize];
        int dataread;

        if ( (dataread = fis.read(chunkBytes)) <= 0) {
            System.out.println("error leyendo chunkBytes: " + nChunk);
        }

        return dataread;
    }

    private void sendChunk(int nChunk) throws Exception {

        int chunkSize = getChunk(nChunk, FTPService.CHUNKSIZE);
        this.out.write(this.chunkBytes, 0, chunkSize);
        System.out.println("file: " + this.f.getPath() + " chunkBytes sent: " + nChunk);
        sendUDPOK(this.rHost, this.rPortUDP, "chunk: " + nChunk);
    } //when sending one chunk

    private void sendChunk(int firstChunk, int lastChunk) throws Exception {

        int i;
        for (i = firstChunk; i <= lastChunk; i++) {

            int chunkSize = getChunk(i, FTPService.CHUNKSIZE);
            this.out.write(this.chunkBytes, 0, chunkSize);
            System.out.println("file: " + this.f.getPath() + " chunk sent: " + i);
            sendUDPOK(this.rHost, this.rPortUDP, "chunk: " + i);
        }

    }//when sending chunk interval

    private void sendFile(File f) throws Exception{

        System.out.println("Sending file: " + f.getPath());

        byte buffer[] = new byte[10000000];

        int dataLength;
        while ((dataLength = this.fis.read(buffer)) > 0) {
            this.out.write(buffer, 0, dataLength);
        }
        System.out.println("data sent");
    }

    private void sendUDPOK(InetAddress rHost, int rPortUDP) throws Exception {

        String text = FTPService.Response.OK.toString();
        byte[] buff = text.getBytes();

        DatagramSocket d = new DatagramSocket(); // SO elige puerto
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rHost, rPortUDP);
        d.send(packet);
        System.out.println("Sent: " + text);
    }//will be deprecated soon

    private void sendUDPOK(InetAddress rHost, int rPortUDP, String message) throws Exception {
        /*
        use when confirming specific file or action
         */
        String text = FTPService.Response.OK.toString() + " " + message;
        byte[] buff = text.getBytes();

        DatagramSocket d = new DatagramSocket(); // SO elige puerto
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rHost, rPortUDP);
        d.send(packet);
        System.out.println("Sent: " + text);
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
