package com.shihui.nio;

public class TimeServer {
    public static void main(String[] args) {
        int port = 8080;
        MultipleTimeServer timeServer = new MultipleTimeServer(port);
        new Thread(timeServer, "NIO-TimeServer-001").start();
    }
}
