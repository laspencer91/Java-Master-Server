package com.gloomy;

import com.gloomy.server.Server;

import java.net.*;

class Entry
{
    public static void main(String[] args)
    {
        Server mainServer = Server.Create(3223);
        mainServer.start();

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(3224, InetAddress.getLocalHost());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
