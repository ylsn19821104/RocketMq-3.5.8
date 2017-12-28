package com.alibaba.rocketmq.example.quickstart;

/**
 * Created by hongxp on 2017/5/13.
 */
public class PerfamenceTest {
    public static void main(String[] args) {
        doTest();
    }


    public static void doTest() {
        int nLoops = 10;
        double l=0;
        long then = System.currentTimeMillis();
        for (int i = 0; i < nLoops; i++) {
            l = fibImpl1(40);
        }
        long now = System.currentTimeMillis();
        System.err.println("Result:" + l + ",Elapsed time:" + (now - then));
    }

    private static double fibImpl1(int n) {
        if (n < 0)
            throw new IllegalArgumentException("Must be > 0");
        if (n == 0)
            return 0d;
        if (n == 1)
            return 1d;
        double d = fibImpl1(n - 2) + fibImpl1(n - 1);
        if (Double.isInfinite(d))
            throw new ArithmeticException("Overflow");
        return d;
    }
}
