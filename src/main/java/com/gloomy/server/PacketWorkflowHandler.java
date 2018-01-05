package com.gloomy.server;

import com.gloomy.session.Session;
import com.gloomy.session.Client;
import com.gloomy.utils.GloomyNetMessageBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketWorkflowHandler
{
    private LinkedBlockingQueue<DatagramPacket> packetsToBeWorked;

    private boolean working;

    public static String TYPE_OF_PACKET_KEY = "T:P";

    public static String CLIENT_NAME_PACKET_KEY = "name";

    public static String SESSION_ID_KEY = "sId";

    public static String CLIENT_ID_KEY  = "cId";

    private Server owningServer;

    /**
     * Creates a workflow handler
     * @param owningServer the server that created this workflow handler belongs to
     */
    public PacketWorkflowHandler(Server owningServer)
    {
        this.owningServer = owningServer;
    }

    /**
     * Add a packet to the workflow handlers work queue
     * @param packet
     */
    public void addWork(DatagramPacket packet)
    {
        packetsToBeWorked.add(packet);
    }

    /**
     * Starts the workflow handler thread
     */
    public void start()
    {
        working = true;
        packetsToBeWorked = new LinkedBlockingQueue<>();
        Thread workThread = new Thread(this::handleWork);
        workThread.start();

        System.out.println("Workflow handler has started working");
    }

    /**
     * The main workflow handler thread. Waits for packets to arrive in queue
     * then puts them through packet processing
     */
    private void handleWork()
    {
        DatagramPacket packetToHandle = null;

        while (working)
        {
            try {
                // Wait for packets to be placed into the queue. Process "sleeps here"
                packetToHandle = packetsToBeWorked.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            processPacket(packetToHandle);
        }
    }

    /**
     * Handle and route the various packets types
     * @param packet the packet to handle
     */
    private void processPacket(DatagramPacket packet)
    {
        // Route the packet to handle through the workflow
        Map<String, Object> receivedMessage = getMessageMapFromPacket(packet);
        InetAddress senderIp = packet.getAddress();
        int senderPort = packet.getPort();
        int packetType = ((Double)receivedMessage.get(TYPE_OF_PACKET_KEY)).intValue();

        switch (packetType)
        {
            case PacketType.MATCH_REQUEST:  // Join Game If Session Is Available Or Host If Not
            {
                String senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                Session openSession = owningServer.findOpenSession();

                if (openSession != null)
                    openSession.addClient(new Client(senderName, senderIp, senderPort));           // Join Game
                else
                    owningServer.createNewSession(new Client(senderName, senderIp, senderPort));   // Host a game

                break;
            }
            case PacketType.CLIENT_DISCONNECT:
            {
                int senderSessionId = ((Double) receivedMessage.get(SESSION_ID_KEY)).intValue();
                String senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                Session sessionToDisconnectClient = owningServer.getSession(senderSessionId);
                sessionToDisconnectClient.disconnectClient(new Client(senderName, senderIp, senderPort));
                break;
            }
            default:
                Logger.warn("Unknown Packet type received. Ignoring");
                break;
        }
    }

    /**
     * Convert the Data contained in a received packet into a useable Map
     * @param packet The packet to parse
     * @return A mapping for String message keys to message values
     */
    private Map<String, Object> getMessageMapFromPacket(DatagramPacket packet)
    {
        byte[] data = packet.getData();
        Gson gson = new Gson();

        String receivedString = new String(data).replace("\"", "'").trim();
        System.out.println(receivedString);
        return gson.fromJson(receivedString, new TypeToken<Map<String, Object>>(){}.getType());
    }

    /**
     * Shut down the work processors thread
     */
    public void shutDown()
    {
        working = false;
    }
}
