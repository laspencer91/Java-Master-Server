package com.gloomy;

import com.gloomy.server.Server;

import java.net.*;

public class Entry
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

        byte[] garbage = {1, 4 ,2, 3, 5, 6, 7, 4};

        //try {
            //socket.send(new DatagramPacket(garbage, garbage.length, InetAddress.getLocalHost(), 3223));
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
    }
}
