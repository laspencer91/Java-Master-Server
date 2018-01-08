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
    private int team;
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

    public void setTeam(int team) { this.team = team; }

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

    public int getTeam() { return team; }

    public byte getClientId() {
        return clientId;
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
            infoMap.put("ip", ip.getHostAddress());
            infoMap.put("port", port);
            infoMap.put("team", team);
            infoMap.put("cId", clientId);
        }

        return infoMap;
    }
}
