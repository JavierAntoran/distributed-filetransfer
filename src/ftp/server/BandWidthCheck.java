package ftp.server;

import ftp.FTPService;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by StFrancisco on 11/04/2017.
 */
public class BandWidthCheck extends HandleFTPConnection {



    public BandWidthCheck(int lPort, InetAddress rHost, int rPort) {
        super(lPort, rHost, rPort);
    }

    @Override
    public void run() {
        System.out.println("Bandwidth measuring TCP connection established");

        try {
            establishTCP();
            int i;
            int nTimes = FTPService.BWCHECKSIZE; //Mb
            int timeLapsed = 0;

            byte buf[] = new byte[FTPService.CHUNKSIZE];
            Arrays.fill(buf, (byte) 8);

            for(i = 0; i < nTimes; i++) {
                this.out.write(buf, 0, FTPService.CHUNKSIZE);
            }
            this.out.close();

            sendUDPOK(null);


        } catch (IOException iox) {}

    }
}

