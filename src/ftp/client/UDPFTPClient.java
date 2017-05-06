/*
* AUTOR: ALBERTO MUR LOPEZ
* NIA:
* FICHERO: UDPFTPClient.java
* TIEMPO: 3
* DESCRIPCIÓN: Gestión de las acciones que realiza un cliente sobre un
* servidor UDP-FTP
*/

package ftp.client;

import ftp.FTPService;

import java.io.*;
import java.net.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class UDPFTPClient {

    Scanner sc = new Scanner(System.in);


    public static void main(String [] args) {

        switch (args.length) {
            case 0:
                new UDPFTPClient("localhost", FTPService.SERVERPORT);
                break;

            case 1:
                new UDPFTPClient(args[0],FTPService.SERVERPORT);
                break;

            default:
                new UDPFTPClient(args[0],Integer.parseInt(args[1]));
                break;
        }
    }

    public UDPFTPClient(String host, int port) {

        DatagramSocket dSocket;
        DatagramPacket dgp;
        InetSocketAddress sa;


        String command;
        String response;

        System.out.println("Welcome to UDPFTP Client\n\n");

        try {

            sa = new InetSocketAddress(InetAddress.getByName(host), port);

            dSocket = new DatagramSocket();
            dSocket.setSoTimeout(5000);

            System.out.println("Opening connection to "
                    + sa.getAddress().getHostAddress() + ":" + port);

            //System.out.println("> HELLO");

            FTPService.sendUDPmessage(dSocket, "HELLO", sa);

            dgp = new DatagramPacket(new byte [FTPService.SIZEMAX],
                    FTPService.SIZEMAX);

            dSocket.receive(dgp);

            response = FTPService.stringFromDatagramPacket(dgp);

            //System.out.println ("< " + response);

            if (FTPService.responseFromString(response) == FTPService.Response.WCOME) {

                do {
                    System.out.print("ftp> ");
                    command = sc.nextLine();

                    FTPService.sendUDPmessage(dSocket, command, sa);

                    dgp.setLength(FTPService.SIZEMAX);
                    dSocket.receive(dgp);

                    response = FTPService.stringFromDatagramPacket(dgp);
                    // Print response
                    // System.out.println ("< " + response);

                    switch(FTPService.commandFromString(command)){
                        case LIST:
                            if ( FTPService.responseFromString(response) == FTPService.Response.PORT ) {

                                listCommand(host,FTPService.portFromResponse(response));

                                dSocket.receive(dgp);

                                System.out.println ("< " + FTPService.stringFromDatagramPacket(dgp));

                            } else {
                                System.out.println("[ERROR] PORT command not received");
                            }
                            break;
                        case GET:
                            if ( FTPService.responseFromString(response) == FTPService.Response.PORT ) {

                                if ( getCommand(host,
                                                FTPService.portFromResponse(response),
                                                FTPService.requestedFile(command)        ) ) {

                                    dSocket.receive(dgp);
                                    System.out.println("< " + FTPService.stringFromDatagramPacket(dgp));
                                }
                            } else {
                                System.out.println("[ERROR] PORT command not received");
                            }
                            break;
                    }

                } while (FTPService.commandFromString(command) != FTPService.Command.QUIT);
            } else {
                System.out.println ("Received unexpected meesage. Closing connection.");
            }

            dSocket.close();
        } catch (SocketException e) {

            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public void listCommand(String host, int port) {
        try {
            Socket s = new Socket(host, port);
            InputStream in = s.getInputStream();
            byte[] data = new byte[FTPService.SIZEMAX];
            int dataLength;
            String list = "";
            while ( (dataLength = in.read(data)) != -1 ) {
                list += new String(data,0,dataLength);
            }

            System.out.println("Size    \tFilename");
            System.out.println("--------------------");
            for (String line: list.split("\n")) {
                String fileName, fileSize;
                fileName = line.split("\t")[0];
                fileSize = line.split("\t")[1];
                System.out.println(
                        String.format("%1$8s\t%2$s",
                                fileSize,
                                fileName));
            }
            in.close();
            s.close();
        }catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean getCommand(String host, int port, String filename) {
        Path file;
        InputStream in;
        OutputStream out;

        long startTime, elapsedTime;

        try {
            if (Files.exists(Paths.get(filename))) {
                System.out.print(String.format("The file \"%s\" exists. Replace it? Y/N: ",filename ));
                String answer = this.sc.nextLine();
                if (answer.equalsIgnoreCase("N")) {
                    return false;
                }
            }
            Files.deleteIfExists(Paths.get(filename));

            startTime = System.nanoTime();

            file = Files.createFile(Paths.get(filename));
            Socket s = new Socket(host, port);
            in = s.getInputStream();
            out = Files.newOutputStream(file);

            byte[] data = new byte[FTPService.CHUNKSIZE];

            int dataLength;
            long off = 0;
            while ( (dataLength = in.read(data)) != -1 ) {
                out.write(data,0,dataLength);
                off = off + dataLength;
            }
            System.out.println("Received " + off + " bytes");
            in.close();
            out.close();
            s.close();

            elapsedTime = System.nanoTime() - startTime;

            FTPService.logInfo(String.format("Elapsed time: %f seconds", elapsedTime*1e-9));

        } catch (FileAlreadyExistsException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
