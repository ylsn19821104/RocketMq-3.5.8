package com.shihui.common.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by hongxp on 2017/6/4.
 */
public class JedisUtil {
    private static String ADDR = "127.0.0.1";
    private static int PORT = 6379;
    private static int MAX_ACTIVE = 1024;

    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 200;

    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 10000;

    private static int TIMEOUT = 10000;

    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;

    private static JedisPool jedisPool = null;

    static {
        JedisPoolConfig conf = new JedisPoolConfig();
        conf.setMaxIdle(MAX_IDLE);
        conf.setTestOnBorrow(TEST_ON_BORROW);
        jedisPool = new JedisPool(conf, ADDR, PORT, TIMEOUT, null);
    }

    public static Jedis getJedis() {
        if (jedisPool != null)
            return jedisPool.getResource();
        return null;
    }

    public static void returnResource(final Jedis jedis) {
        if (jedis != null)
            jedis.close();
    }
}
