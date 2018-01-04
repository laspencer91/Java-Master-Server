package com.gloomy.server;

public class PacketType
{
    public static final byte HOST_REQUEST = 0;
    public static final byte JOIN_REQUEST = 1;
    public static final byte HOST_SUCCESS = 2;
    public static final byte JOIN_SUCCESS = 3;
    public static final byte JOIN_FAILED  = 4;
    public static final byte ALL_CLIENT_INFO = 5;
    public static final byte CLIENT_REMOVED = 6;
}
