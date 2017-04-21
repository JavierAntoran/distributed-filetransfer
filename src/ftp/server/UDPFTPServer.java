package ftp.server;

import ftp.FTPService;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by StFrancisco on 21/03/2017.
 */
public class UDPFTPServer {

    static final String FTP_ROOT = "ftp_files/";

    private ExecutorService executor;

    private DatagramSocket s;
    private DatagramPacket p;

    public static void main(String[] args) {
        Scanner input;
        int port;

        if (!Files.isReadable(Paths.get(FTP_ROOT))) {
            System.out.println("FTP Root is not readable");
            System.exit(1);
        }

        switch (args.length) {
            case 0:
                input = new Scanner(System.in);
                System.out.printf("Introduce el numero de puerto (default:%d): ", FTPService.SERVERPORT);
                port = input.nextInt();
                new UDPFTPServer(port);
                break;
            case 1:
                int serverUDPPort = Integer.parseInt(args[0]);
                new UDPFTPServer(serverUDPPort);
                break;
            default:
                System.out.println("invalid number of arguments");
                System.exit(2);
        }
    }

    public UDPFTPServer(int port) {

        executor = Executors.newFixedThreadPool(FTPService.MAXSERVERTHREADS);

        try{

            s = new DatagramSocket(port); //Este escucha
            p = new DatagramPacket(new byte[FTPService.SIZEMAX], FTPService.SIZEMAX);

            System.out.println("Server is listening on " + s.getLocalPort());

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

    private void listAction() {
        File dir = new File(FTP_ROOT);
        try {
            if (Files.isReadable(dir.toPath())) {
                executor.execute(new HandleList(dir, 0, this.p.getAddress(), this.p.getPort()));
            } else {
                String rx = FTPService.Response.SERVERROR.toString();
                rx += " " + dir.getPath() + " is not readable";
                FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
                return;
            }
        }catch (Exception e){}
    }

    private void getAction(String request) throws IOException {
        String rx;
        String file;
        int[] chunkIntval;

        try {

            if ((file = FTPService.getFilenameFromPart(FTPService.requestedFile(request))) != null){
                chunkIntval = FTPService.getIntervalFromPart(request);
            } else {
                file = FTPService.requestedFile(request);
                chunkIntval = new int[] {0,0};
            }

            File f = new File(FTP_ROOT + file);

            if (f.exists() && !f.isDirectory()) {
                executor.execute( new HandleChunk(f, chunkIntval[0], chunkIntval[1],
                        0, this.p.getAddress(), this.p.getPort()) );

            }
            else {
                rx = FTPService.Response.SERVERROR.toString();
                rx += " " + f.getPath() + " does not exist or is a folder";
                FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
                return;
            }


        } catch (Exception e) {
            rx = FTPService.Response.SERVERROR.toString();
            rx += " " + e.getMessage();
            FTPService.sendUDPmessage(this.s, rx, this.p.getSocketAddress());
            return;
        }
    }

    /*private void checkBWAction() {
        executor.execute(new BandWidthCheck(0, this.p.getAddress(), this.p.getPort()));
    }*/

    private void handleCommand(){
        String sRX = new String(p.getData(),0, p.getLength());
        FTPService.Command cRX = FTPService.commandFromString(sRX);
        try {
            switch (cRX) {
                case HELLO:
                    this.helloAction();
                    break;

                case LIST:
                    this.listAction();
                    break;

                case GET:
                    this.getAction(sRX);
                    break;

                /*case CHECKBW:
                    this.checkBWAction();
                    break;
                    */

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
