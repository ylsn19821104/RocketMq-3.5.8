package com.shihui;

import com.shihui.common.util.JedisUtil;
import com.shihui.common.util.SerializeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hongxp on 2017/6/4.
 */
public class RedisTest {
    public static void main(String[] args) {
        Map<String, String> carMap = JedisUtil.getJedis().hgetAll("car:1");
        for (String v : carMap.values())
            System.err.println("" + v);
    }

    public static void test() {
        List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("number", "王小二");
        map.put("name", "王二");
        list.add(map);

        HashMap<String, String> map2 = new HashMap<String, String>();
        map2.put("number", "张三");
        map2.put("name", "张小三");
        list.add(map2);


        JedisUtil.getJedis().setex("test:list".getBytes(), 60, SerializeUtil.serializeToBytes(list));

        byte[] bytes = JedisUtil.getJedis().get("test:list".getBytes());
        if (bytes != null && bytes.length > 0) {
            List<HashMap<String, String>> deList = (List<HashMap<String, String>>) SerializeUtil.unSerialize(bytes);
            if (deList != null && deList.size() > 0) {
                for (Map m : deList) {
                    System.out.println(m.get("number") + "--" + m.get("name"));
                }
            }
        }
    }
}
