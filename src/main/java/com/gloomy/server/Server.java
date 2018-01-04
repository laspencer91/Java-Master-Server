package com.gloomy.server;

import com.gloomy.session.Session;
import com.gloomy.session.Client;
import org.pmw.tinylog.Configurator;

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

    /**
     * Start the server
     */
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
        //System.out.println("Server is listening for packets at " + socket.getPort() + " " + socket.getInetAddress().getHostAddress());

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
     * Creates a new game session if a session from the host does not already exist
     * @param host The Client instance representing the host of the session
     */
    public synchronized void createNewSession(Client host)
    {
        if (sessions.contains(host) || availableSessionIds.isEmpty())
            return;

        Session newSession = Session.Create(host,this).setSessionId(availableSessionIds.poll());
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
}
