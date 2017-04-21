package ftp.client.Session;

import ftp.FTPService;
import ftp.client.tcp.ClientTCPHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * Created by StFrancisco on 10/04/2017.
 */
public class RemoteServer{

    private String name;
    private long bw;
    private InetAddress addr;
    private int port;

    private boolean isUP = true; //used to mark for deletion if down

    public RemoteServer(String name, InetAddress addr, int port, long bw) {
        this.addr = addr;
        this.name = name;
        this.bw = bw;
        this.addr = addr;
        this.port = port;
    }

    public RemoteServer(InetAddress addr, int port, long bw) {
        this.addr = addr;
        this.name = addr.getHostName() + ":" + port;
        this.bw = bw;
        this.addr = addr;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public String toString(){
        return this.name;
    }

    public int getBw() {
        return bw;
    }

    public void setBw(long bw) {
        this.bw = bw;
    }

    public boolean isUP() {
        return isUP;
    }

}
