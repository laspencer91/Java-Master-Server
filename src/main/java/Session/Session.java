package Session;

import Server.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Session
{
    private Server owningServer;
    private List<Client> clients;
    private Client host;
    private int maxClients = 6;
    private BlockingQueue<SessionTask> sessionTasks;

    private final static String MESSAGE_TYPE = "TYPE";

    private Session() {}

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

    private Session(Client host, Server owningServer)
    {
        this.host = host;
        this.owningServer = owningServer;
        clients = new ArrayList<>();
        clients.add(this.host);

        System.out.println("New Session.Session Has Been Created..");
        System.out.println("Host Name: " + host.getUserName());
        System.out.println("Host Ip: " + host.getIp().getHostName());
        System.out.println("Host Port: " + host.getPort());

        sessionTasks = new LinkedBlockingQueue<>();
        Thread sessionThread = new Thread(() -> processTasks());
        sessionThread.start();
    }

    /**
     * Process tasks on own thread that have to do with this session
     */
    private void processTasks()
    {
        SessionTask task = null;

        while (true)
        {
            try { task = sessionTasks.take(); } catch (InterruptedException e) { e.printStackTrace();  }

            switch (task.messageType)
            {
                case ADD_CLIENT:
                    // TODO Handle a client added process (Send all clients data of new user)
                    break;
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
            clients.add(newClient);
            sessionTasks.add(new SessionTask(MessageType.ADD_CLIENT));
            System.out.println("Session.Client " + newClient.getUserName() + " has been added to " + host.getUserName() + "'s session.");
        }
        else
        {
            System.out.println("Session.Client " + newClient.getUserName() + " has tryed to join more than once, not added to session " + host.getUserName());
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

    /**
     * @return true if this session is available to have clients join
     */
    public boolean isOpen()
    {
        return clients.size() < maxClients;
    }
}
