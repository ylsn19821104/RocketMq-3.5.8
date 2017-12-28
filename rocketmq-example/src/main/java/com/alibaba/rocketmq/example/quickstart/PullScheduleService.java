package com.alibaba.rocketmq.example.quickstart;

import com.alibaba.rocketmq.client.consumer.*;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

/**
 * Created by hongxp on 2017/12/15.
 */
public class PullScheduleService {
    public static void main(String[] args) throws MQClientException {
        final MQPullConsumerScheduleService scheduleService = new MQPullConsumerScheduleService("Group1");
        scheduleService.getDefaultMQPullConsumer().setNamesrvAddr("localhost:9876");
        scheduleService.setMessageModel(MessageModel.CLUSTERING);
        scheduleService.registerPullTaskCallback("TopicTest", new PullTaskCallback() {
            @Override
            public void doPullTask(MessageQueue mq, PullTaskContext context) {
                MQPullConsumer consumer = context.getPullConsumer();
                try {
                    long offset = consumer.fetchConsumeOffset(mq, false);
                    if (offset < 0) {
                        offset = 0;
                    }
                    PullResult pullResult = consumer.pull(mq, "*", offset, 32);
                    System.err.println(offset + "\t" + mq + "\t" + pullResult);
                    switch (pullResult.getPullStatus()) {
                        case NO_MATCHED_MSG:
                        case FOUND:
                        case OFFSET_ILLEGAL:
                        case NO_NEW_MSG:
                            break;
                        default:
                            break;
                    }
                    // 存储Offset，客户端每隔5s会定时刷新到Broker
                    consumer.updateConsumeOffset(mq, pullResult.getNextBeginOffset());
                    // 设置再过100ms后重新拉取
                    context.setPullNextDelayTimeMillis(100);
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
        });
        scheduleService.start();
    }
}
