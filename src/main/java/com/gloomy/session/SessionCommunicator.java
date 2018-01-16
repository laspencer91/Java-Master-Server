package com.gloomy.session;

import com.gloomy.server.PacketType;
import com.gloomy.utils.GloomyNetMessageBuilder;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class uses a thread for sending outgoing messages to  the specified recipients using a BlockingQueue
 * A SessionCommunicator is owned by a Session, and messages to be send are added to the communicator. The
 * Communicators thread will then pick these messages out of the queue and send them.
 */
public class SessionCommunicator
{

    private final List<Client> sessionClients;

    private final DatagramSocket socket;

    private final BlockingQueue<SessionMessage> messageSendList = new LinkedBlockingQueue<>();

    private boolean working;

    /**
     * Create a session communicator
     * @param owningSession The session that this communicator belongs to
     * @param sessionSocket The socket that should be used to send data through
     */
    public SessionCommunicator(Session owningSession, DatagramSocket sessionSocket)
    {
        Session owningSession1 = owningSession;
        this.sessionClients = owningSession.getClients();
        this.socket         = sessionSocket;

        // Start processing this sessions commands
        working = true;
        Thread sessionThread = new Thread(this::processOutgoingMessages);
        sessionThread.start();
    }

    /** Process tasks on own thread that have to do with this session **/
    private void processOutgoingMessages()
    {
        SessionMessage message = null;

        if (socket == null) {
            Logger.warn("Socket is null for the Session, not processing commands..");
            return;
        }
        working = true;

        while (working)
        {
            try { message = messageSendList.take(); } catch (InterruptedException e) { e.printStackTrace();  }

            byte[] messageData = message.message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(messageData, messageData.length);

            // Send recipients the message
            synchronized (sessionClients)
            {
                for (Client client : message.recipients)
                {
                    sendPacket.setAddress(client.getIp());
                    sendPacket.setPort(client.getPort());
                    sendDatagramPacket(sendPacket);
                }
            }
        }
    }

    /***
     * Adds a new SessionMessage to the messageQueue with the specified clients as recipients
     * @param message The SessionMessage to send
     * @param recipients Clients to send the message to
     */
    public void addMessage(SessionMessage message, Client... recipients)
    {
        message.setRecipients(recipients);
        Logger.info("Recipients amount: {}", recipients.length);
        messageSendList.add(message);
    }

    /**
     * Adds a message to the message queue using a list of clients as recipients
     * @param message The Sessionmessage to send
     * @param recipients List of Clients to send to
     */
    public void addMessage(SessionMessage message, List<Client> recipients)
    {
        message.setRecipients(recipients);
        messageSendList.add(message);
    }

    /** Creates a message with all Client information to be sent to all clients */
    /** Splits the client info if message size gets too long                    */
    public void addBroadcastAllClientsInfoMessage()
    {
        final int MAX_USERS_PER_PACKET = 6;

        if (sessionClients.size() > MAX_USERS_PER_PACKET)
            addBroadcastClientInfoMessagesSplit(MAX_USERS_PER_PACKET);
        else
            addBroadcastClientInfoMessageAsOnePacket();
    }

    /** Simply sends all Users Info in a Single Message Packet */
    private void addBroadcastClientInfoMessageAsOnePacket()
    {
        GloomyNetMessageBuilder builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
        List<Map<String, Object>> clientInfo = new ArrayList<>();

        synchronized (sessionClients)
        {
            for (Client client : sessionClients)
                clientInfo.add(client.getInfoMap());
        }

        builder.addData("clients", clientInfo);
        String finalInfo = builder.build();
        addMessageToBeBroadcasted(finalInfo);

        Logger.info(finalInfo);
        Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
    }

    /** This is used to break client info into multiple packets if needed. Sending too large of packets
     * may result in unwanted behaviour
     * @param maxUsersPerPacket Maximum users info to include in a single packet
     */
    private void addBroadcastClientInfoMessagesSplit(int maxUsersPerPacket)
    {
        GloomyNetMessageBuilder builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
        List<Map<String, Object>> clientInfo = new ArrayList<>();

        synchronized (sessionClients)
        {
            for (int i = 0; i < sessionClients.size(); i++)
            {
                Client client = sessionClients.get(i);
                clientInfo.add(client.getInfoMap());

                if (i != 0 && (i / (maxUsersPerPacket - 1)) % 1 == 0)
                {
                    builder.addData("clients", clientInfo);
                    String finalInfo = builder.build();
                    addMessageToBeBroadcasted(finalInfo);
                    builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
                    clientInfo.clear();
                    Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
                }
            }
        }

        if (!clientInfo.isEmpty())
        {
            builder.addData("clients", clientInfo);
            String finalInfo = builder.build();
            addMessageToBeBroadcasted(finalInfo);
            Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
        }
    }

    /**
     * Adds a message to be broadcasted to all clients to the outgoing message queue
     * @param jsonMessage Message to send to clients
     */
    private void addMessageToBeBroadcasted(String jsonMessage)
    {
        messageSendList.add(new SessionMessage(jsonMessage, sessionClients));
        Logger.info("Packets Sending.. Broadcasting To All Clients: {}", jsonMessage);
    }

    /**
     * Send a packet using the servers socket
     * @param packet Prebuilt packet to send
     */
    private void sendDatagramPacket(DatagramPacket packet)
    {
        if (socket == null)
        {
            Logger.warn("Socket is null, not sending datagram packet from this sesson..");
            return;
        }

        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shut down this session and its thread
     */
    public void shutdown()
    {
        working = false;
    }
}
