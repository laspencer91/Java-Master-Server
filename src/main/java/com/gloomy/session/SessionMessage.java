package com.gloomy.session;

import com.gloomy.server.PacketType;
import com.gloomy.utils.GloomyNetMessageBuilder;

import java.util.Arrays;
import java.util.List;

class SessionMessage
{
    public List<Client> recipients;

    public final String message;

    /**
     * Creates a new SessionMessage based on the provided string that will be sent to the specified
     * recipients
     * @param message The string message to send over the net
     * @param recipients Variable amount of clients that should recieve the message
     */
    public SessionMessage(String message, Client... recipients)
    {
        this.recipients = Arrays.asList(recipients);
        this.message = message;
    }

    /**
     * Creates a new message with a previously created message and a list of recipients
     * @param message A previously created string to send over the net
     * @param recipients List of Clients To Send To
     */
    public SessionMessage(String message, List<Client> recipients)
    {
        this.recipients = recipients;
        this.message = message;
    }

    /**
     * Creates a message but does not specify an recipients to send to.
     * @param message The message to send.
     */
    public SessionMessage(String message)
    {
        this.message = message;
    }

    public void setRecipients(Client... recipients) {
        this.recipients = Arrays.asList(recipients);
    }

    public void setRecipients(List<Client> recipients) {
        this.recipients = recipients;
    }

    /**
     * Builds and returns a SessionMessage with a notification of a client being disconnected
     * @param client The client that has disconnected
     * @return SessionMessage that should be sent to need to know clients
     */
    public static SessionMessage SendOtherDisconnectMessage(Client client) {
        return new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.CLIENT_DISCONNECTED)
                .addData("cId", client.getClientId())
                .build());
    }

    /**
     * Builds a message to be sent to a user notifying them of their disconnection from the server
     * @return SessionMessage to be sent to a user notifying them of their disconnect
     */
    public static SessionMessage SendYouDisconnectedMessage() {
        return new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.YOURE_DISCONNECTED).build());
    }

    /**
     * Constructs a join success message that should be sent to a client or multiple clients
     * @param client The client that successfully joined
     * @param isHost If the client specified is the host or not
     * @param sessionId The id of the session this client belongs to.
     * @return SessionMessage with the needed content for the join success message
     */
    public static SessionMessage JoinSuccessMessage(Client client, boolean isHost, int sessionId)
    {
        return new SessionMessage(GloomyNetMessageBuilder.Create(PacketType.JOIN_SUCCESS).addData("host", isHost)
                .addData("cId", client.getClientId())
                .addData("sId", sessionId)
                .addData("team", client.getTeam().id)
                .build());
    }
}
