package com.alibaba.rocketmq.example.quickstart;

import com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer;
import com.alibaba.rocketmq.client.consumer.PullResult;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * Created by hongxp on 2017/12/15.
 */
public class PullConsumer {
    private static final Map<MessageQueue, Long> offsetTale = Maps.newHashMap();

    public static void main(String[] args) {
        try {
            DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("PullConsumer");
            consumer.setNamesrvAddr("localhost:9876");
            consumer.start();

            Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues("TopicTest");
            for (MessageQueue mq : mqs) {
                System.err.println("Consume from the queue: " + mq);
                SINGLE_MQ:
                while (true) {
                    PullResult pullResult = consumer.pullBlockIfNotFound(mq, null, getMessageOffset(mq), 32);
                    System.err.println(pullResult);
                    putMessageQueueOffset(mq, pullResult.getNextBeginOffset());
                    switch (pullResult.getPullStatus()) {
                        case FOUND:
                            // TODO
                            break;
                        case NO_MATCHED_MSG:
                            break;
                        case NO_NEW_MSG:
                            break SINGLE_MQ;
                        case OFFSET_ILLEGAL:
                            break;
                        default:
                            break;
                    }
                }
            }
            consumer.shutdown();
        } catch (MQClientException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RemotingException e) {
            e.printStackTrace();
        } catch (MQBrokerException e) {
            e.printStackTrace();
        }

    }

    private static void putMessageQueueOffset(MessageQueue mq, long offset) {
        offsetTale.put(mq, offset);
    }

    public static long getMessageOffset(MessageQueue mq) {
        Long offset = offsetTale.get(mq);
        if (offset != null) {
            return offset;
        }
        return 0;
    }
}
