package ftp.client.Session;

import java.net.InetAddress;

/**
 * Created by StFrancisco on 10/04/2017.
 */
public class RemoteServer {
    private int bw;
    private InetAddress addr;
    private int port;

    public RemoteServer(InetAddress addr, int port, int bw) {
        this.bw = bw;
        this.addr = addr;
        this.port = port;
    }

    public int getBw() {
        return bw;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }
}
