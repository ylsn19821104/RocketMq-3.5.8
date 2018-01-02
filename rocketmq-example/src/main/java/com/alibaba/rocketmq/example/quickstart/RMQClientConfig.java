package com.alibaba.rocketmq.example.quickstart;

import com.alibaba.rocketmq.client.ClientConfig;

/**
 * RocketMQ单例配置实现
 */
public class RMQClientConfig {
    private RMQClientConfig() {
    }

    private static class Singleton {
        private static ClientConfig clientConfig = new ClientConfig();

        static {
            // 客户端本机 IP 地址，某些机器会发生无法识别客户端IP地址情况，需要应用在代码中强制指定
            clientConfig.setClientIP("192.168.28.94");
            // Name Server 地址列表，多个 NameServer 地址用分号 隔开
            clientConfig.setNamesrvAddr("192.168.28.94:9876;192.168.28.95:9876;");
            // 客户端实例名称，客户端创建的多个 Producer、 Consumer 实际是共用一个内部实例（这个实例包含
            // 网络连接、线程资源等）,默认值:DEFAULT
            clientConfig.setInstanceName("DEFAULT");
            // 通信层异步回调线程数,默认值4
            clientConfig.setClientCallbackExecutorThreads(10);
            // 轮询 Name Server 间隔时间，单位毫秒,默认：30000
            clientConfig.setPollNameServerInteval(30000);
            // 向 Broker 发送心跳间隔时间，单位毫秒,默认：30000
            clientConfig.setHeartbeatBrokerInterval(30000);
            // 持久化 Consumer 消费进度间隔时间，单位毫秒,默认：5000
            clientConfig.setPersistConsumerOffsetInterval(10000);
        }
    }

    /**
     * 获取一个配置实例
     *
     * @return
     * @throws
     * @MethodName: getInstance
     * @Description:
     */
    public ClientConfig getInstance() {
        return Singleton.clientConfig;
    }

    /**
     * 克隆一个客户端配置
     */
    public ClientConfig clone() {
        return Singleton.clientConfig.cloneClientConfig();
    }

    /**
     * 重置客户端配置
     *
     * @param clientConfig
     * @throws
     * @MethodName: reset
     * @Description:
     */
    public void reset(ClientConfig clientConfig) {
        Singleton.clientConfig.resetClientConfig(clientConfig);
        ;
    }

    /**
     * 重写toString方法
     */
    @Override
    public String toString() {
        return Singleton.clientConfig.toString();
    }
}
