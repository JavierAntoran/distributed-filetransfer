package ftp.server;

import ftp.FTPService;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class UDPFTPServer {

    static final String FTP_ROOT = "ftp/";

    private ExecutorService executor;

    private DatagramSocket s;
    private DatagramPacket p;

    public static void main(String[] args) {

        if (!Files.isReadable(Paths.get(FTP_ROOT))) {
            System.out.println("FTP Root is not readable");
            System.exit(1);
        }

        switch (args.length) {
            case 0:
                new UDPFTPServer(FTPService.SERVERPORT);
                break;
            case 1:
                new UDPFTPServer(Integer.parseInt(args[0]));
                break;
        }
    }

    public UDPFTPServer(int port) {

        executor = Executors.newFixedThreadPool(20);

        try{

            s = new DatagramSocket(port); //Este escucha
            p = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            System.out.println("Server is listening on " + port);

            while (true) {

                s.receive(p);

                System.out.println("Received " + p.getAddress().getHostAddress()
                        + ":" + p.getPort() + " "
                        + new String(p.getData(),0, p.getLength()));

                this.handleCommand();

                p.setLength(FTPService.SIZEMAX);
            }

        }catch (SocketException e) {
            System.out.println("Unable to bind port " + port);
            System.exit(1);
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
            System.exit(1);
        }
    }

    private void helloAction() throws IOException{
        String rx = FTPService.Response.WCOME.toString();

        rx = rx + " Welcome to DUFTP Server";
        FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
    }

    private void quitAction() throws IOException{
        String rx = FTPService.Response.BYE.toString();
        FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
    }

    private void listAction(String request) {
        File file;
        file = new File(FTP_ROOT);
        try {
            if (Files.isReadable(file.toPath())) {
                executor.execute(new HandleList(file, 0, this.p.getAddress(), this.p.getPort()));
            } else {
                String rx = FTPService.Response.SERVERROR.toString();
                rx += " " + file.getPath() + " is not readable";
                FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
                return;
            }
        }catch (Exception e){}
    }

    private void getAction(String request) throws IOException {
        String rx;;
        String file;
        int[] chunkIntval;
        try {
            file = FTPService.requestedFile(request);
        } catch (Exception e) {
            rx = FTPService.Response.SERVERROR.toString();
            rx += " " + e.getMessage();
            FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
            return;
        }

        // TODO implement Get   Handle
    }

    private void handleCommand(){
        String sRX = new String(p.getData(),0, p.getLength());
        FTPService.Command cRX = FTPService.commandFromString(sRX);
        try {
            switch (cRX) {
                case HELLO:
                    this.helloAction();
                    break;

                case LIST:
                    this.listAction(sRX);
                    break;

                case GET:
                    this.getAction(sRX);
                    break;

                case QUIT:
                    this.quitAction();
                    break;

                default:
                    FTPService.sendUDPmessage(this.s,
                            FTPService.Response.UNKNOWN.toString(),
                            this.p.getSocketAddress());
                    break;
            }
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
        }
    }

}
