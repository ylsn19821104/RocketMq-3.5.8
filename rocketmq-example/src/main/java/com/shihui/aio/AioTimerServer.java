package com.shihui.aio;

public class AioTimerServer {
    public static void main(String[] args) {
        int port = 8080;
        AsyncTimeServerHandler timeServer = new AsyncTimeServerHandler(port);
        new Thread(timeServer, "AIO-TimeServer-001").start();
    }
}
