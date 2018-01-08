import com.gloomy.server.Server;
import com.gloomy.session.Client;
import com.gloomy.session.Session;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;

public class SessionTest
{

    public  SessionTest()
    {
        Server testServer = Server.Create(3223);
        Client testHost = null;
        try {
            testHost = new Client("Session Test", InetAddress.getByName("127.0.0.1"), 3224);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        testServer.createNewSession(testHost);
        Session testSession = testServer.getSessions().get(0);

        try {
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3227 ));
            testSession.addClient(new Client("TestClient 2",InetAddress.getByName("127.0.0.1"), 3228 ));
            testSession.addClient(new Client("TestClient 3",InetAddress.getByName("127.0.0.1"), 3229 ));
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3230 ));
            testSession.addClient(new Client("TestClient 2",InetAddress.getByName("127.0.0.1"), 3231 ));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSessionSendClientInfo()
    {
        Server testServer = Server.Create(3223);
        Client testHost = null;
        try {
            testHost = new Client("Session Test", InetAddress.getByName("127.0.0.1"), 3224);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        testServer.createNewSession(testHost);
        Session testSession = testServer.getSessions().get(0);

        try {
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3227 ));
            testSession.addClient(new Client("TestClient 2",InetAddress.getByName("127.0.0.1"), 3228 ));
            testSession.addClient(new Client("TestClient 3",InetAddress.getByName("127.0.0.1"), 3229 ));
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3230 ));
            testSession.addClient(new Client("TestClient 2",InetAddress.getByName("127.0.0.1"), 3231 ));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        testSession.getCommunicator().addBroadcastAllClientsInfoMessage();
    }

    @Test
    public void testSessionDuplicateClientAddAttempt()
    {
        Server testServer = Server.Create(3223);
        Client testHost = null;
        try {
            testHost = new Client("Session Test", InetAddress.getByName("127.0.0.1"), 3224);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        testServer.createNewSession(testHost);
        Session testSession = testServer.getSessions().get(0);

        try {
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3227 ));
            testSession.addClient(new Client("TestClient 2",InetAddress.getByName("127.0.0.1"), 3228 ));
            testSession.addClient(new Client("TestClient 3",InetAddress.getByName("127.0.0.1"), 3229 ));
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3230 )); // DUPLICATE
            testSession.addClient(new Client("TestClient 1",InetAddress.getByName("127.0.0.1"), 3230 )); // DUPLICATE
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        int clientSize = testSession.getClients().size();
        System.out.println(clientSize);
        assertTrue(clientSize == 5);
    }
}
