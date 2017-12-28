package com.alibaba.rocketmq.example.quickstart;

/**
 * Created by hongxp on 2017/5/2.
 */
public class JsonFilterTest {
    public static void main(String[] args) {
        float money = 12.20f;
        float post = 2.20f;
        String strSum = "订单总额:%f (含运费%f)";
        strSum = String.format(strSum, money, post);
        System.err.println(strSum);
    }
}
