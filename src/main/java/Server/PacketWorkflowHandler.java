package Server;

import Session.Session;
import Session.Client;
import Utils.GloomyNetMessageBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.DatagramPacket;
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
        Thread workThread = new Thread(() -> handleWork());
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
                // Wait for packets to be placed into the queue. Process
                // "sleeps here"
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
        int packetType = (int) receivedMessage.get(TYPE_OF_PACKET_KEY);

        int senderPort = packet.getPort();
        InetAddress senderIp = packet.getAddress();

        String senderName;

        switch (packetType)
        {
            case (PacketType.HOST_REQUEST): /// User requested to host a new session
                senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                owningServer.createNewSession(new Client(senderName, senderIp, senderPort));

                // TODO Send the sender a message back saying the session is created
                break;

            case (PacketType.JOIN_REQUEST): /// Sender requesting to join a game
                senderName = (String) receivedMessage.get(CLIENT_NAME_PACKET_KEY);
                Session openSession = owningServer.findOpenSession();

                if (openSession != null)
                {
                    openSession.addClient(new Client(senderName, senderIp, senderPort));
                }
                else
                {
                    System.out.println("No Sessions Available");
                    String sendMessage = GloomyNetMessageBuilder.Create(2).build();
                    //TODO Send the sender a message saying no session available
                }

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
        GsonBuilder messageParser = new GsonBuilder();
        Gson gson = new Gson();

        return gson.fromJson(messageParser.create().toJson(new String(data)), Map.class);
    }

    /**
     * Shut down the work processors thread
     */
    public void shutDown()
    {
        working = false;
    }
}
