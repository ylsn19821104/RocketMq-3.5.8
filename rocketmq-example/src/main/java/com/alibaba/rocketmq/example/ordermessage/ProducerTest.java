package com.alibaba.rocketmq.example.ordermessage;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

import java.util.List;

/**
 * Created by hongxp on 2017/5/18.
 */
public class ProducerTest {
    public static void main(String[] args) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        DefaultMQProducer producer = new DefaultMQProducer("Producer");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.setInstanceName("dd");
        producer.setRetryTimesWhenSendFailed(3);
        producer.start();

        Message msg = new Message("PushTopic", "push", "1", "内容一".getBytes());
        SendResult sendResult = producer.send(msg);
        producer.send(msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.err.println(sendResult.getSendStatus());
                System.err.println("成功了");
            }

            @Override
            public void onException(Throwable e) {
                System.err.println("失败了" + e.getMessage());
            }
        });

        msg = new Message("PushTopic", "push", "2", "内容二".getBytes());
        producer.send(msg);

        List<MessageQueue> queues = producer.fetchPublishMessageQueues("PushTopic");
        System.err.println(queues.size());

        producer.shutdown();

    }
}
