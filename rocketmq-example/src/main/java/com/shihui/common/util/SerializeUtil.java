package com.shihui.common.util;

import java.io.*;

/**
 * Created by hongxp on 2017/6/4.
 */
public class SerializeUtil {
    public static byte[] serializeToBytes(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);

            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
        }

        return null;
    }

    public static Object unSerialize(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static String serialize(Object obj) {
        String rt = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(obj);
            rt = out.toString("utf-8");

            oos.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rt;
    }

    public static Object deSerialize(String str) {
        Object obj = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("utf-8"));
            ObjectInputStream oin = new ObjectInputStream(in);
            obj = oin.readObject();

            oin.close();
            in.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
