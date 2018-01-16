package com.gloomy.server;

public class PacketType
{
    public static final byte JOIN_SUCCESS = 0;
    public static final byte ALL_CLIENT_INFO = 1;
    public static final byte CLIENT_DISCONNECTED = 2;
    public static final byte MATCH_REQUEST = 3;
    public static final byte CLIENT_DISCONNECT = 4;
    public static final byte YOURE_DISCONNECTED = 5;
    public static final byte HOST_CHANGE        = 6;
    public static final byte GAME_READY = 7;
    public static final byte GAME_START = 10;
}
