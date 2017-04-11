package ftp.client.Session;

import ftp.FTPService;
import ftp.client.ClientTCPHandler;
import ftp.client.UDPFTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import static ftp.client.UDPFTPClient.executor;

/**
 * Created by StFrancisco on 10/04/2017.
 */
public class RemoteServer extends ClientTCPHandler{
    private int bw;
    private InetAddress addr;
    private int port;

    private boolean isUP = true; //used to mark for deletion if down

    public RemoteServer(InetAddress addr, int port, int bw) {

        super(0, addr, port);

        this.bw = bw;
        this.addr = addr;
        this.port = port;

    }

    public RemoteServer(int lPort, InetAddress rHost, int rPort) {
        super(lPort, rHost, rPort);
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getBw() {
        return bw;
    }

    public boolean isUP() {
        return isUP;
    }

    public void getBandWidth(int rPortTCP) {
        this.rPort = rPortTCP;
        executor.execute(this);
    }


    @Override
    public void run() {

        if (rPort == port) {
            System.out.println("error: set rPort for tcp");
        } else {
            try {

                int dataLength;
                long dataRead = 0;
                byte buff[] = new byte[FTPService.CHUNKSIZE];

                establishTCP();
                InputStream dataStream = stream.getInputStream();
                long startTime = System.nanoTime();
                while ((dataLength = dataStream.read(buff)) != -1) {
                    dataRead += dataLength;
                    //CHECSystem.out.println(dataRead);
                }

                if (dataRead != FTPService.BWCHECKSIZE * FTPService.CHUNKSIZE) {
                    System.out.println("error in number of bytes read");
                    //TODO: do something

                } else {
                    long endTime = System.nanoTime();

                    long timeLapsed = endTime - startTime;
                    long bw = (dataRead * 1000000000 / timeLapsed);
                    this.bw = (int) bw;
                    System.out.println(this.addr.getHostName() +
                            ": bandwidth updated to: " +  ((float) bw / 1000000));
                }

                dataStream.close();
                stream.close();

            } catch (IOException serverDown) {
                System.out.println(serverDown);
                this.isUP = false;

            }
        }
    }

}
