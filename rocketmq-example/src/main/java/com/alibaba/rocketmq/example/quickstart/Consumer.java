/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.rocketmq.example.quickstart;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 1.DefaultMQPushConsumer.start()开始
 * <p>
 * 2.RebalanceService.run()方法定时调用RebalanceImpl.doRebalance()方法,该方法内部是遍历订阅的topic,执行rebalanceByTopic(topic)
 * <p>
 * 3.调用RebalanceImpl.updateProcessQueueTableInRebalance(),构造PullRequest,从Broker获取nextOffset,pullRequest.setNextOffset(nextOffset),同时更新本地消费进度记录
 * <p>
 * 4.调用RebalancePushImpl.dispatchPullRequest(List)
 * <p>
 * 5.调用PullMessageService.executePullRequestImmediately(final PullRequest)放入pullRequestQueue队列中去
 * <p>
 * 6.PullMessageService.run()从pullRequestQueue队列中取出PullRequest,调用DefaultMQPushConsumerImpl.pullMessage(pullRequest)作拉取消息的动作
 * <p>
 * <p>
 * 7.构造回调函数PullCallback,对拉取消息结果PullResult做处理,具体是,从PullResult中解码出拉取的消息列表,如果消息的订阅tag不为空且不是classFilter过滤模式,则进行tag过滤,然后把过滤后的消息列表装入PullResult,取出pullResult的nextBeginOffset装入当前的pullRequest的NextOffset中,更新统计数据,异步提交ConsumeRequest进行消息消费,接着提交pullRequest准备做下一次拉取消息的请求
 * <p>
 * 8.DefaultMQPushConsumerImpl.pullAPIWrapper.pullKernelImpl(//
 * pullRequest.getMessageQueue(), // 1
 * subExpression, // 2
 * subscriptionData.getSubVersion(), // 3
 * pullRequest.getNextOffset(), // 4
 * this.defaultMQPushConsumer.getPullBatchSize(), // 5
 * sysFlag, // 6
 * commitOffsetValue,// 7
 * BrokerSuspendMaxTimeMillis, // 8
 * ConsumerTimeoutMillisWhenSuspend, // 9
 * CommunicationMode.ASYNC, // 10
 * pullCallback// 11
 * );
 * <p>
 * ######到此Consumer端发消息结束######
 * <p>
 * ———我是分割线———-
 * <p>
 * ######接着Request走到Broker######
 * <p>
 * 9.PullMessageProcessor.processRequest()接收到拉消息的请求,做一些简单的判断,如检查Broker权限,确保订阅组存在,
 * 检查topic是否存在,然后去messageStore里取消息
 * <p>
 * 详细说明：DefaultMessageStore根据请求的Topic和queueId获取对应的ConsumerQueue,根据传入的queueOffset从consumerQueue里取出目标buffer,
 * 然后以20个字节为单位循环从目标buffer里取,取出偏移量offsetPy（占8个字节）,消息长度sizePy（占4个字节),过滤标识tagCode（占8个字节）,
 * 判断如果订阅信息匹配tagCode,则以offsetPy和sizePy从commitLog中以取出消息体buffer,存入GetMessageResult,
 * 然后再进行下一次取,最后返回GetMessageResult
 * <p>
 * 10.取出GetMessageResult的NextBeginoffset,minOffset,maxOffet3个属性,设置到responseHeader中,
 * 然后把GetMessageResult打包进response后发送到Consumer端
 * <p>
 * ######到此Broker端结束######
 * <p>
 * ———我是分割线———-
 * <p>
 * ######GetMessageResult又走到Consumer######
 * <p>
 * 11.接着response到Consumer端,就会执行前面说的PullCallback里的操作这里展开消息消费说一下,就是前面提到的,异步提交ConsumeRequest进行消息消费：
 * <p>
 * DefaultMQPushConsumerImpl.this.consumeMessageService.submitConsumeRequest(//
 * pullResult.getMsgFoundList(), //
 * processQueue, //
 * pullRequest.getMessageQueue(), //
 * dispathToConsume);
 * <p>
 * 说明：
 * consumeMessageService这里是ConsumeMessageConcurrentlyService
 * listener就是consumer在start之前注册的listener
 * <p>
 * consumer.registerMessageListener(new MessageListenerConcurrently() {
 *
 * @Override public ConsumeConcurrentlyStatus consumeMessage(List msgs,
 * ConsumeConcurrentlyContext context) {
 * System.out.println(Thread.currentThread().getName() + ” Receive New Messages: ” + msgs);
 * System.out.println(” Receive Message Size: ” + msgs.size());
 * return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
 * }
 * });
 * <p>
 * 是多线程并行消费
 */
public class Consumer {

    public static void main(String[] args) throws InterruptedException, MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name_4");
        consumer.setNamesrvAddr("127.0.0.1:9876");

        /**
         * 设置Consumer第一次启动是从队列头部开始消费还是队列尾部开始消费
         * 如果非第一次启动,那么按照上次消费的位置继续消费
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("Shuihui_TopicTest", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                //业务端可以通过检查msgId,来控制重复消费
                messageExt.getMsgId();
                System.out.println(Thread.currentThread().getName() + " Receive New Messages: " + new String(msgs.get(0).getBody()));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();

        System.out.println("Consumer Started.");
    }
}
