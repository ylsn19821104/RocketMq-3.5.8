package com.shihui;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by hongxp on 2017/11/23.
 */
public class IOServer {
    public static void main(String[] args) {
        server();
    }

    private static void server() {
        ServerSocket serverSocket = null;
        InputStream in = null;

        try {
            serverSocket = new ServerSocket(8080);
            int recvMsgSize;
            byte[] recvBuf = new byte[1024];
            while (true) {
                Socket socket = serverSocket.accept();
                SocketAddress clientAddress = socket.getRemoteSocketAddress();
                System.err.println("Handling client at:" + clientAddress);
                in = socket.getInputStream();
                while ((recvMsgSize = in.read(recvBuf)) != -1) {
                    byte[] tmp = new byte[recvMsgSize];
                    System.arraycopy(recvBuf, 0, tmp, 0, recvMsgSize);
                    System.err.println("client message is:" + new String(tmp));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }

                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
