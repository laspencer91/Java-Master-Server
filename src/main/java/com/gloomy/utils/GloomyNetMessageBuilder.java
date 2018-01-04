package com.gloomy.utils;

import com.gloomy.server.PacketWorkflowHandler;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class GloomyNetMessageBuilder
{
    private int messageType = -1;

    private Map<String, Object> messageMap;

    private GloomyNetMessageBuilder() {}

    private GloomyNetMessageBuilder(int messageType)
    {
        this.messageType = messageType;

        messageMap = new HashMap<>();
        messageMap.put(PacketWorkflowHandler.TYPE_OF_PACKET_KEY, messageType);
    }

    /**
     * Creates and returns a new message builder initialized with the message type specified
     * @param messageType The type of message that this packet is. Should be a byte
     * @return A GloomyNetMessageBuilder instance
     */
    public static GloomyNetMessageBuilder Create(int messageType)
    {
        return new GloomyNetMessageBuilder(messageType);
    }

    /**
     * Add a piece of data to the message. This is a Key -> Value pair where where a key must be unique.
     * @param key String key to store
     * @param value Object mapped to by the key
     * @return This instance of GloomyNetMessageBuilder
     */
    public GloomyNetMessageBuilder addData(String key, Object value)
    {
        messageMap.put(key, value);
        return this;
    }

    /**
     * Sets the message type of this message.
     * @param messageType The type to set this message. An integer value
     * @return This instance of the GloomyNetMessageBuilder
     */
    public GloomyNetMessageBuilder setMessageType(int messageType)
    {
        this.messageType = messageType;
        return this;
    }

    /**
     * Converts the Mappings into a Json string that can be sent over the network.
     * @return The Json String created from this instances mapping.
     */
    public String build()
    {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().toJson(messageMap);
    }
}
