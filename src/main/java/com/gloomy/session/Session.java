package com.gloomy.session;

import com.gloomy.server.Server;
import com.gloomy.utils.GloomyNetMessageBuilder;
import com.gloomy.server.PacketType;
import org.pmw.tinylog.Logger;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Session
{
    private Server owningServer;
    private List<Client> clients;
    private ConcurrentLinkedQueue<Byte> clientIds;
    private Client host;
    private int maxClients = 6;
    private int sessionId;
    private BlockingQueue<SessionMessage> messageSendList;
    private DatagramSocket socket;

    /** Use Session.Create instead **/
    private Session() {}

    /**
     * Create a new session using the provided client as the host
     * @param host Client instance to set as the host of this session
     * @param owningServer The server that this session belongs to
     * @return The created Session instance
     */
    public static Session Create(Client host, Server owningServer)
    {
        return Session.Create(host, owningServer, 6);
    }

    /**
     * Create a new instance of Session
     * @param host The Client instance that represents the host of the session
     * @param owningServer The server that owns this session
     * @param maxClients the maximum amount of clients that can be in this session
     * @return The Session instance that was created
     */
    public static Session Create(Client host, Server owningServer, int maxClients)
    {
        Session newSession = new Session(host, owningServer);
        newSession.setMaxClients(maxClients);
        return newSession;
    }

    /**
     * Creates a new session. This method is called internally from a static Create method.
     * @param host Instance of Client that is host of this server
     * @param owningServer The server that this session belongs to
     */
    private Session(Client host, Server owningServer)
    {
        // Initialize Client Id Stack
        clientIds = new ConcurrentLinkedQueue<>();
        messageSendList = new LinkedBlockingQueue<>();
        for (byte i = 0; i < maxClients; i++)
            clientIds.add(i);

        this.host = host;
        this.host.setClientId(clientIds.poll());
        this.owningServer = owningServer;
        this.socket = owningServer.getSocket();
        this.clients = new ArrayList<>();
        this.clients.add(this.host);

        Logger.info("New Session Has Been Created.. \n  Host Name: {}\n  Host Ip: {}\n  Host Port: {}",
                host.getUserName(), host.getIp().getHostName(), host.getPort());

        // Send Host Success Message and Initialize messageSendList
        String hostSuccessMessage = GloomyNetMessageBuilder.Create(PacketType.HOST_SUCCESS).build();
        messageSendList.add(new SessionMessage(hostSuccessMessage, host));

        // Start processing this sessions commands
        Thread sessionThread = new Thread(this::processTasks);
        sessionThread.start();
    }

    /**
     * Process tasks on own thread that have to do with this session
     */
    private void processTasks()
    {
        SessionMessage message = null;

        if (socket == null) {
            Logger.warn("Socket is null for the Session, not processing commands..");
            return;
        }

        while (true)
        {
            try { message = messageSendList.take(); } catch (InterruptedException e) { e.printStackTrace();  }

            byte[] messageData = message.message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(messageData, messageData.length);

            // Send recipients the message
            for (Client client : message.recipients)
            {
                sendPacket.setAddress(client.getIp());
                sendPacket.setPort(client.getPort());

                sendDatagramPacket(sendPacket);
            }
        }
    }

    /**
     * Add a new client to this exception if that client is not already added. Add a task to the task list
     * so that we can send all other clients the client data. (Possibly?)
     * @param newClient Client instance to be added
     */
    public void addClient(Client newClient)
    {
        if (!clients.contains(newClient))
        {
            newClient.setClientId(clientIds.poll()); // Get client Id
            clients.add(newClient);
            SessionMessage newMessage =
                    new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.JOIN_SUCCESS)
                                                              .addData("clients", clients.size())
                                                              .build(), newClient);
            messageSendList.add(newMessage);
            Logger.info("Client {} has been added to {}'s session.", newClient.getUserName(), host.getUserName());
        }
        else
        {
            Logger.info("Client {} has tryed to join more than once, not added to session {}", newClient.getUserName(), host.getUserName());
        }
    }

    /**
     * Remove a client from our session
     * @param client The client to remove
     */
    public void removeClient(Client client)
    {
        if (!clients.contains(client))
        {
            clientIds.add(client.getClientId());
            clients.remove(client);
            SessionMessage newMessage =
                    new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.CLIENT_REMOVED)
                            .addData("clients", clients.size())
                            .build(), clients);
            messageSendList.add(newMessage);
            Logger.info("Client {} has been removed from {}'s session.", client.getUserName(), host.getUserName());
        }
        else
        {
            Logger.warn("Attempted to remove client {} that does not exist in this session", client.getUserName(), host.getUserName());
        }
    }

    /**
     * @return List of Clients in this session
     */
    public List<Client> getClients()
    {
        return clients;
    }

    /**
     * @return Client instance that is host of this session
     */
    public Client getHost()
    {
        return host;
    }

    public Session setMaxClients(int maxClients)
    {
        this.maxClients = maxClients;
        return this;
    }

    public Session setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * @return true if this session is available to have clients join
     */
    public boolean isOpen()
    {
        return clients.size() < maxClients;
    }

    // --------------------------------------------------------------
    //                MESSAGE SENDERS
    // --------------------------------------------------------------

    /** Sends a message to all clients including every clients information */
    /** Splits the client info if message size gets too long               */
    public void broadcastAllClientsInfo()
    {
        final int MAX_USERS_PER_PACKET = 6;

        if (clients.size() > MAX_USERS_PER_PACKET)
            broadcastClientInfoWithMaxLength(MAX_USERS_PER_PACKET);
        else
            broadcastClientInfoInOnePacket();
    }

    /** Simply sends all Users Info in a Single Message Packet */
    private void broadcastClientInfoInOnePacket()
    {
        GloomyNetMessageBuilder builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
        List<Map<String, Object>> clientInfo = new ArrayList<>();

        for (Client client : clients)
        {
            clientInfo.add(client.getInfoMap());
        }

        builder.addData("clients", clientInfo);
        String finalInfo = builder.build();
        broadcastMessage(finalInfo);
        Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
    }

    /** This is used to break client info into multiple packets if needed. Sending too large of packets
     * may result in unwanted behaviour
     * @param maxUsersPerPacket Maximum users info to include in a single packet
     */
    private void broadcastClientInfoWithMaxLength(int maxUsersPerPacket)
    {
        GloomyNetMessageBuilder builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
        List<Map<String, Object>> clientInfo = new ArrayList<>();

        for (int i = 0; i < clients.size(); i++)
        {
            Client client = clients.get(i);
            clientInfo.add(client.getInfoMap());

            if (i != 0 && (i / (maxUsersPerPacket - 1)) % 1 == 0)
            {
                builder.addData("clients", clientInfo);
                String finalInfo = builder.build();
                broadcastMessage(finalInfo);
                builder = GloomyNetMessageBuilder.Create(PacketType.ALL_CLIENT_INFO);
                clientInfo.clear();
                Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
            }
        }

        if (!clientInfo.isEmpty())
        {
            builder.addData("clients", clientInfo);
            String finalInfo = builder.build();
            broadcastMessage(finalInfo);
            Logger.info("Packet Size For Broadcast is: {}", finalInfo.getBytes().length);
        }
    }

    /**
     * Send a message to all clients in this session (including host)
     * @param jsonMessage Message to send to clients
     */
    private void broadcastMessage(String jsonMessage)
    {
        messageSendList.add(new SessionMessage(jsonMessage, clients));
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
}