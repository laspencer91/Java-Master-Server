package Session;

import java.net.InetAddress;

public class Client
{
    private String userName;
    private InetAddress ip;
    private int port;

    private Client() {}

    public Client(String userName, InetAddress ip, int port)
    {
        this.userName = userName;
        this.ip = ip;
        this.port = port;
    }


    public String getUserName() {
        return userName;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort()
    {
        return port;
    }

    public boolean equals(Client o) {
        return (userName.equals(o.getUserName()) && port == o.getPort() && ip.getAddress().equals(o.getIp().getAddress()));
    }
}
