package com.gloomy.server;

import com.gloomy.session.Session;
import com.gloomy.session.Client;
import com.gloomy.utils.GloomyNetMessageBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    private static String CLIENT_NAME_PACKET_KEY = "name";

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

        Double recievedDouble = (Double)receivedMessage.get(TYPE_OF_PACKET_KEY);
        int packetType = recievedDouble.intValue();
        int senderPort = packet.getPort();
        InetAddress senderIp = packet.getAddress();
        String senderName;

        switch (packetType)
        {
            case PacketType.HOST_REQUEST: /// User requested to host a new session
                senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                owningServer.createNewSession(new Client(senderName, senderIp, senderPort));
                break;
            case PacketType.JOIN_REQUEST: /// Sender requesting to join a game
                senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                Session openSession = owningServer.findOpenSession();

                if (openSession != null)
                {
                    openSession.addClient(new Client(senderName, senderIp, senderPort));
                }
                else
                {
                    System.out.println("No Sessions Available");

                    // Send No Session Available Message To Client
                    String sendMessage = GloomyNetMessageBuilder.Create(PacketType.JOIN_FAILED).build();
                    DatagramSocket socket = owningServer.getSocket();
                    DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, senderIp, senderPort);
                    try { socket.send(sendPacket); } catch (IOException e) { e.printStackTrace(); }
                }
                break;
            default:
                System.out.println("Kyle Called You a BICHHHHH");
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
