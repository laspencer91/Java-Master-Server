package Session;

import java.util.Arrays;
import java.util.List;

class SessionMessage
{
    public final List<Client> recipients;

    public final String message;

    public SessionMessage(String message, Client... recipients)
    {
        this.recipients = Arrays.asList(recipients);
        this.message = message;
    }

    public SessionMessage(String message, List<Client> recipients)
    {
        this.recipients = recipients;
        this.message = message;
    }
}
