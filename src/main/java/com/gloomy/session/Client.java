package com.gloomy.session;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Client
{
    private String userName;
    private InetAddress ip;
    private int port;
    private byte clientId;
    private Map<String, Object> infoMap;

    private Client() {}

    public Client(String userName, InetAddress ip, int port)
    {
        this.userName = userName;
        this.ip = ip;
        this.port = port;
    }

    public void setClientId(byte clientId)
    {
        this.clientId = clientId;
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

    @Override
    public boolean equals(Object o) {
        Client cl = (Client) o;
        return (userName.equals(cl.getUserName()) && port == cl.getPort() && ip.getHostAddress().equals(cl.getIp().getHostAddress()));
    }

    public Map<String, Object> getInfoMap() {
        if (infoMap == null) {
            infoMap = new HashMap<>();
            infoMap.put("name", userName);
            infoMap.put("id", clientId);
            infoMap.put("ip", ip.getHostAddress());
            infoMap.put("port", port);
        }

        return infoMap;
    }

    public byte getClientId() {
        return clientId;
    }
}
