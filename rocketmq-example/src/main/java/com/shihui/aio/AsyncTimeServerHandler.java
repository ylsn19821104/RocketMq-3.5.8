package com.shihui.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class AsyncTimeServerHandler implements Runnable {
    int port;
    CountDownLatch latch;
    AsynchronousServerSocketChannel assc;

    public AsyncTimeServerHandler(int port) {
        this.port = port;
        try {
            assc = AsynchronousServerSocketChannel.open();
            assc.bind(new InetSocketAddress(port));
            System.err.println("The time server is start at port:" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        latch = new CountDownLatch(1);
        doAccept();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doAccept() {
        assc.accept(this,
                new AcceptCompletionHandler());
    }
}
