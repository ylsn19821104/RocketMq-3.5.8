package com.shihui;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by hongxp on 2017/11/22.
 */
public class IOTest {
    public static void main(String[] args) {
        //io();
        nio();
    }

    private static void nio() {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("D:\\workspace_java\\rocketmq\\pom.xml", "rw");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(1024);

            int bytesRead = channel.read(buf);
            System.err.println("byte read:" + bytesRead);

            while (bytesRead != -1) {
                buf.flip();
                while (buf.hasRemaining()) {
                    System.err.print((char) buf.get());
                }
                buf.compact();
                bytesRead = channel.read(buf);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void io() {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream("D:\\workspace_java\\rocketmq\\pom.xml"));
            byte[] buf = new byte[1024];
            int bytesRead = in.read(buf);
            while (bytesRead != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    System.err.print((char) buf[i]);
                }
                bytesRead = in.read(buf);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
