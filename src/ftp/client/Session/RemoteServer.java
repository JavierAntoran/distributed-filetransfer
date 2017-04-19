package ftp.client.Session;

import java.net.InetAddress;

/**
 * Created by StFrancisco on 10/04/2017.
 */
public class RemoteServer {

    private String name;
    private int bw;
    private InetAddress addr;
    private int port;

    private int dataPort;

    public RemoteServer(String name, InetAddress addr, int port, int bw) {
        this.name = name;
        this.bw = bw;
        this.addr = addr;
        this.port = port;
    }

    public RemoteServer(InetAddress addr, int port, int bw) {
        this(addr.getHostName(), addr, port, bw);
        this.name = addr.getHostName() + ":" + port;
    }

    public String getName() {
        return name;
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

    public String toString(){
        return this.name;
    }
}
