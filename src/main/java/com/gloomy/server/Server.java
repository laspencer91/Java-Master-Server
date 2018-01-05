package com.gloomy.server;

import com.gloomy.session.Session;
import com.gloomy.session.Client;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server
{
    final int PORT;
    final int DATA_BUFFER_SIZE = 512;
    final int maxSessions = 256;

    byte[] byteBuffer;
    boolean listening;
    DatagramSocket socket;
    DatagramPacket dataPacket;

    List<Session> sessions = Collections.synchronizedList(new ArrayList<>());
    ConcurrentLinkedQueue<Integer> availableSessionIds = new ConcurrentLinkedQueue<>();

    PacketWorkflowHandler packetHandler = new PacketWorkflowHandler(this);

    private Server(int port)
    {
        Configurator.currentConfig().formatPattern("{level}: {message|indent=4}").activate();
        this.PORT = port;

        for (int i = 0; i < maxSessions; i++)
            availableSessionIds.add(i);
    }

    /**
     * Create an instance of a Server listening on the Given Port
     * @param port To Listen on
     * @return
     */
    public static Server Create(int port)
    {
        return new Server(port);
    }

    /** Start the server **/
    public void start()
    {
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Server starting listening at port " + PORT);

        listening = true;
        Thread listenThread = new Thread(() -> listen());
        packetHandler.start();
        listenThread.start();
    }

    private void listen()
    {
        while (listening)
        {
            System.out.println("Listening");
            byteBuffer = new byte[DATA_BUFFER_SIZE];
            dataPacket = new DatagramPacket(byteBuffer, DATA_BUFFER_SIZE);

            try {
                socket.receive(dataPacket);
                System.out.println("Data Packet Recieved");
            } catch (IOException e) {
                e.printStackTrace();
            }

            packetHandler.addWork(dataPacket);
        }
    }

    /**
     * Creates a new game session if available session ids exist
     * @param host The Client instance representing the host of the session
     */
    public synchronized void createNewSession(Client host)
    {
        if (availableSessionIds.isEmpty())
            return;

        Session newSession = Session.Create(host,this, availableSessionIds.poll());
        sessions.add(newSession);

        System.out.println("New Session Has Been Created By " + host.getUserName());
    }

    /**
     * Cycle through sessions and find the first session that is available to have clients join
     * @return The Session instance that is open
     */
    public synchronized Session findOpenSession()
    {
        synchronized (sessions)
        {
            Iterator i = sessions.iterator(); // Must be in synchronized block
            Session sessionToCheck;
            while (i.hasNext())
            {
                sessionToCheck = (Session) i.next();
                if (sessionToCheck.isOpen())
                {
                    return sessionToCheck;
                }
            }

            return null;
        }
    }


    /**
     * Shutdown a specified session belonging to this server
     * @param session The Session instance to shut down
     */
    public void sessionShutdown(Session session) {
        boolean success = sessions.remove(session);
        if (success) {
            availableSessionIds.add(session.getSessionId());
            session.shutdown();
            Logger.info("Session {} has been shutdown.", session.getSessionId());
        }
        else
            Logger.warn("Attempted to remove session that is not found in this servers session list.");
    }

    public int getPort()
    {
        return PORT;
    }

    public List<Session> getSessions()
    {
        return sessions;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * Get the Session instance associated with the specified sessionId
     * @param senderSessionId The session Id to find the Session of
     * @return The found session, or null if the session was not found
     */
    public Session getSession(int senderSessionId) {

        synchronized (sessions) {
            for (Session session: sessions) {
                if (session.getSessionId() == senderSessionId)
                    return session;
            }
            Logger.warn("Host sent disconnect message, but no session found with the given host id");
            return null;
        }
    }

}
