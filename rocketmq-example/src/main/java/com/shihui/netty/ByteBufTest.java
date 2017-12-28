package com.shihui.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

public class ByteBufTest {
    public static void main(String[] args) {
        byte[] byte1 = "he     ".getBytes();
        byte[] byte2 = "llo     ".getBytes();
        ByteBuffer b1 = ByteBuffer.allocate(10);
        b1.put(byte1);
        ByteBuffer b2 = ByteBuffer.allocate(10);
        b2.put(byte2);
        ByteBuffer b3 = ByteBuffer.allocate(20);
        b3.put(b1.array());
        b3.put(b2.array());
        //读取内容
        System.out.println(new String(b3.array()));
        System.out.println("b1 addr:" + b1.array());
        System.out.println("b2 addr:" + b2.array());
        System.out.println("b3 addr:" + b3.array());

        ByteBuf buf1 = Unpooled.buffer(10);
        buf1.writeBytes(byte1);
        ByteBuf buf2 = Unpooled.buffer(10);
        buf2.writeBytes(byte2);

        ByteBuf buf3 = Unpooled.wrappedBuffer(buf1, buf2);
        //buf3.array();
        byte[] rt = new byte[20];
        for (int i = 0; i < buf3.capacity(); i++) {
            rt[i] = buf3.getByte(i);
        }
        System.out.println(new String(rt));
    }
}
