package com.gloomy.session;

import com.gloomy.server.Server;
import com.gloomy.utils.GloomyNetMessageBuilder;
import com.gloomy.server.PacketType;
import org.pmw.tinylog.Logger;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Session
{
    private Server owningServer;
    private List<Client> clients = new ArrayList<>();;
    private ConcurrentLinkedQueue<Byte> clientIds = new ConcurrentLinkedQueue<>();
    private Client host;
    private int maxClients = 6;
    private int sessionId;
    private DatagramSocket socket;
    private SessionCommunicator communicator;


    /** Use Session.Create instead **/
    private Session() {}

    /**
     * Create a new session using the provided client as the host
     * @param host Client instance to set as the host of this session
     * @param owningServer The server that this session belongs to
     * @return The created Session instance
     */
    public static Session Create(Client host, Server owningServer, int sessionId)
    {
        return Session.Create(host, owningServer, sessionId, 6);
    }

    /**
     * Create a new instance of Session
     * @param host The Client instance that represents the host of the session
     * @param owningServer The server that owns this session
     * @param maxClients the maximum amount of clients that can be in this session
     * @return The Session instance that was created
     */
    public static Session Create(Client host, Server owningServer, int sessionId, int maxClients)
    {
        Session newSession = new Session(host, owningServer, sessionId);
        newSession.setMaxClients(maxClients);
        return newSession;
    }

    /**
     * Creates a new session. This method is called internally from a static Create method.
     * @param host Instance of Client that is host of this server
     * @param owningServer The server that this session belongs to
     */
    private Session(Client host, Server owningServer, int sessionId)
    {
        // Initialize Client Id Stack
        for (byte i = 0; i < maxClients; i++)
            clientIds.add(i);

        this.host = host;
        this.host.setClientId(clientIds.poll());
        this.owningServer = owningServer;
        this.socket = owningServer.getSocket();
        this.clients.add(this.host);
        this.communicator = new SessionCommunicator(this, this.socket);
        this.sessionId = sessionId;

        Logger.info("New Session Has Been Created.. \n  Host Name: {}\n  Host Ip: {}\n  Host Port: {}",
                host.getUserName(), host.getIp().getHostName(), host.getPort());

        // Send Host Success Message and Initialize messageSendList
        String hostSuccessMessage = GloomyNetMessageBuilder.Create(PacketType.HOST_SUCCESS)
                                                           .addData("sId", sessionId)
                                                           .build();

        communicator.addMessage(new SessionMessage(hostSuccessMessage), host);
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
                                                              .addData("cId", newClient.getClientId())
                                                              .addData("sId", sessionId)
                                                              .build());
            communicator.addMessage(newMessage, newClient);
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
        if (clients.contains(client))
        {
            clientIds.add(client.getClientId());
            clients.remove(client);
            SessionMessage newMessage =
                    new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.CLIENT_DISCONNECTED)
                                                              .addData("clients", clients.size())
                                                              .build());
            SessionMessage disconnectedMessage =
                    new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.YOURE_DISCONNECTED).build());

            communicator.addMessage(newMessage, clients);
            communicator.addMessage(disconnectedMessage, client);

            if (clients.size() <= 0)
                owningServer.sessionShutdown(this);

            Logger.info("Client {} has been removed from {}'s session.", client.getUserName(), host.getUserName());
        }
        else
        {
            Logger.warn("Attempted to remove client {} that does not exist in this session {}",
                        client.getUserName(),
                        host.getUserName());
        }
    }

    /**
     * @return List of Clients in this session
     */
    public List<Client> getClients()
    {
        return clients;
    }

    public SessionCommunicator getCommunicator() {
        return communicator;
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

    /**
     * Set sessionId of this session
     * @param sessionId
     * @return This Session instance
     */
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

    public int getSessionId() {
        return sessionId;
    }

    /**
     * Disconnect the host, and shut down the session if noone is available to take it over
     */
    private void disconnectHost()
    {
        clients.remove(host);

        if (clients.isEmpty())
        {
            Logger.info("Host disconnected and there are no more available clients in this lobby, shutting server down");
            owningServer.sessionShutdown(this);
        }
        else
        {
            host = clients.get(0);
            Logger.info("Host disconnected, new host is {}, {}", host.getClientId(), host.getUserName());
            // TODO Send new host migration message
        }
    }


    public void disconnectClient(Client client)
    {
        if (client.equals(host))
            disconnectHost();
        else
            removeClient(client);
    }

    public void shutdown() {
        communicator.shutdown();
        Logger.info("Session {} is shutting down..", sessionId);
    }
}