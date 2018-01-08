package com.gloomy.session;

import com.gloomy.server.Server;
import com.gloomy.utils.GloomyNetMessageBuilder;
import com.gloomy.server.PacketType;
import org.pmw.tinylog.Logger;

import java.awt.*;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Session
{
    private static final int DEFAULT_TEAMS       = 2;
    private static final int DEFAULT_MAX_CLIENTS = 6;

    private Server owningServer;
    private List<Client> clients = new ArrayList<>();
    private ConcurrentLinkedQueue<Byte> clientIds = new ConcurrentLinkedQueue<>();
    private Client host;
    private TeamManager teamManager;
    private int maxClients = 6;
    private int sessionId;
    private DatagramSocket socket;
    private SessionCommunicator communicator;
    private int maxTeams;

    /** Use Session.Create instead **/
    private Session() {}

    /**
     * Create a new session using the provided client as the host
     * @param host Client instance to set as the host of this session
     * @param owningServer The server that this session belongs to
     * @param sessionId A unique session identifier for this session
     * @return The created Session instance
     */
    public static Session Create(Client host, Server owningServer, int sessionId)
    {
        return new Session(host, owningServer, sessionId, DEFAULT_MAX_CLIENTS, DEFAULT_TEAMS);
    }

    /**
     * Create a new instance of Session with configured clients and teams
     * @param host The Client instance that represents the host of the session
     * @param owningServer The server that owns this session
     * @param sessionId A unique id for this session
     * @param maxClients the maximum amount of clients that can be in this session
     * @param maxTeams The amount of teams to for this session to use
     * @return The Session instance that was created
     */
    public static Session CreateAndConfigure(Client host, Server owningServer, int sessionId, int maxClients, int maxTeams)
    {
        Session newSession = new Session(host, owningServer, sessionId, maxClients, maxTeams);
        return newSession;
    }

    private Session(Client host, Server owningServer, int sessionId, int maxClients, int maxTeams)
    {
        // Initialize Client Id Stack
        for (byte i = 0; i < maxClients; i++)
            clientIds.add(i);

        this.maxClients   = maxClients;
        this.maxTeams     = maxTeams;
        this.host         = host;
        this.owningServer = owningServer;
        this.socket       = owningServer.getSocket();
        this.sessionId    = sessionId;
        this.communicator = new SessionCommunicator(this, this.socket);
        this.teamManager  = new TeamManager(clients, maxClients, maxTeams);

        Logger.info("New Session Has Been Created.. \n  Host Name: {}\n  Host Ip: {}\n  Host Port: {}",
                host.getUserName(), host.getIp().getHostName(), host.getPort());

        // Add client as host
        addClient(host, true);
    }

    /**
     * Add a new client to this exception if that client is not already added. Add a task to the task list
     * so that we can send all other clients the client data. (Possibly?)
     * @param newClient Client instance to be added
     */
    private void addClient(Client newClient, boolean hosting)
    {
        if (!clients.contains(newClient))
        {
            newClient.setClientId(clientIds.poll()); // Get client Id
            clients.add(newClient);

            newClient.setTeam(teamManager.addClientGetTeam(newClient)); // Set clients team and retrieve their team

            SessionMessage joinMessage = SessionMessage.JoinSuccessMessage(newClient, hosting, sessionId);

            communicator.addMessage(joinMessage, newClient);
            communicator.addBroadcastAllClientsInfoMessage();
            Logger.info("Client {} has been added to {}'s session.", newClient.getUserName(), host.getUserName());
        }
        else
        {
            Logger.info("Client {} tryed to join more than once, not added to session {}", newClient.getUserName(), host.getUserName());
        }
    }

    // Call addClient but set host to false. Used for backwards compatibility
    public void addClient(Client newClient) {
        addClient(newClient, false);
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

            communicator.addMessage(SessionMessage.SendYouDisconnectedMessage(), clients);
            communicator.addMessage(SessionMessage.SendOtherDisconnectMessage(client), client);

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

    /** Disconnect the host, and shut down the session if noone is available to take it over **/
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
            SessionMessage newHostMessage = new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.HOST_CHANGE)
                                                                                      .addData("host", host.getClientId())
                                                                                      .build());
            communicator.addMessage(newHostMessage, clients);
        }
    }

    public void disconnectClient(Client client)
    {
        if (client.equals(host))
            disconnectHost();
        else
            removeClient(client);
    }

    public Client findClientFromId(int cId)
    {
        synchronized (clients)
        {
            for (Client client : clients)
                if (client.getClientId() == cId)
                    return client;

            Logger.warn("Client with id {} in session {} was not found.", cId, sessionId);
            return null;
        }
    }

    public void shutdown()
    {
        communicator.shutdown();
        Logger.info("Session {} is shutting down..", sessionId);
    }
}