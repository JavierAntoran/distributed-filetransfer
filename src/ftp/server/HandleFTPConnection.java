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

    static public enum task {LIST, GET}; //Esto nos marca si mandamos ls o archivo

    private Socket toClient;
    private ServerSocket welcoming;

    private InetAddress lHost;
    private int lPort;
    private InetAddress rHost;
    private int rPortUDP;

    private OutputStream out;
    private task t;
    private File f;
    private String ftpPath;

    public HandleFTPConnection(int lPort, int rPortUDP, String ftpPAth) {


        this.t = task.LIST;
        this.lPort = lPort;
        this.rPortUDP = rPortUDP;
        this.ftpPath = ftpPAth;

        try {
            this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

            this.lPort = welcoming.getLocalPort();
            this.lHost = welcoming.getInetAddress();

        } catch (Exception dx) {};
    }

    public HandleFTPConnection(int lPort, int rPortUDP, File f) {

        this.t = task.GET;
        this.lPort = lPort;
        this.rPortUDP = rPortUDP;
        this.f = f;

        try {
            this.welcoming = new ServerSocket(lPort); // le decimos que escuche donde quiera

            this.lPort = welcoming.getLocalPort();
            this.lHost = welcoming.getInetAddress();

        } catch (Exception dx) {};
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

    private void sendFile(File f) throws Exception{

        System.out.println("Sending file: " + f.getPath());

        byte buffer[] = new byte[10000000];
        FileInputStream fis = new FileInputStream(f);

        int dataLength;
        while ((dataLength = fis.read(buffer)) > 0) {
            this.out.write(buffer, 0, dataLength);
        }
        fis.close();
        this.out.close();
        System.out.println("data sent");
    }

    private void sendUDPOK(InetAddress rHost, int rPortUDP) throws Exception {

        String text = FTPService.Response.OK.toString();
        byte[] buff = text.getBytes();

        DatagramSocket d = new DatagramSocket(); // SO elige puerto
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rHost, rPortUDP);
        d.send(packet);
        System.out.println("Sent: " + text);
    }

    @Override
    public void run() {

        System.out.println("FTP handler launched");
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
            } else if (this.t.equals(task.GET)) {
                sendFile(this.f);
            }

            this.toClient.close();
            this.welcoming.close();

            System.out.println("TCP Conection with " + rHost.toString() + ":" + toClient.getPort() + " closed");

            sendUDPOK(rHost, rPortUDP);

        } catch (Exception fx) {}
    }

}
