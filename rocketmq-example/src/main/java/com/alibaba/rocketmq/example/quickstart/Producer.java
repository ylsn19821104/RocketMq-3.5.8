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

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.*;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.remoting.common.RemotingHelper;

import java.util.List;

/**
 * 1.消息产生后由Producer发送至Broker
 * 2.Broker接收到消息做持久化
 * <p>
 * 调试代码过程:
 * 1.DefaultMQProducer.send()发出消息
 * <p>
 * 2.DefaultMQProducerImpl.sendDefaultImpl()发出消息
 * <p>
 * 3.DefaultMQProducerImpl.tryToFindTopicPublishInfo(),即向Namesrv发出GET_ROUTEINTO_BY_TOPIC的请求,来更新
 * MQProducerInner的topicPublishInfoTable和MQConsumerInner的topicSubscribeInfoTable
 * <p>
 * 4.调用topicPublishInfo.selectOneMessageQueue(),从发布的topic中轮询取出一个MessageQueue默认一个topic对应4个MessageQueue
 * <p>
 * 5.调用mQClientFactory.findBrokerAddressInPublish(mq.getBrokerName()),获取brokerAddr（broker的地址）
 * <p>
 * 6.调用this.mQClientFactory.getMQClientAPIImpl().sendMessage(
 * brokerAddr,// 1
 * mq.getBrokerName(),// 2
 * msg,// 3
 * requestHeader,// 4
 * timeout,// 5
 * communicationMode,// 6
 * sendCallback// 7
 * )发送
 * <p>
 * 7.调用MQClientAPIIImpl.sendMessageSync(addr, brokerName, msg, timeoutMillis, request)发送
 * <p>
 * 8.调用NettyRemotingClient.invokeSyncImpl()发送
 * <p>
 * ######到此Producer端发消息结束 ######
 * <p>
 * ######接着Request走到Broker######
 * 9.SendMessageProcessor.processRequest(),接收到消息,封装requestHeader成broker内部的消息MessageExtBrokerInner,
 * 然后DefaultMessageStore.putMessage(msgInner),调用CommitLog.putMessage(msg)
 * <p>
 * 10.调用MapedFileQueue.getLastMapedFile()获取将要写入消息的文件,mapedFile.appendMessage(msg,this.appendMessageCallback)写入消息
 * <p>
 * 11.AppendMessageCallback.doAppend(fileFromOffset, byteBuffer,maxBlank,Object msg),用回调方法存储msg
 * <p>
 * 12.MessageDecoder.createMessageId(this.msgIdMemory, msgInner.getStoreHostBytes(),wroteOffset),
 * 用存储消息的节点ip和端口,加上准备写的偏移量（就是在前面获取的文件中）生成msgId
 * <p>
 * 13.以（topic-queueId）为key从topicQueueTable取queueOffset,queueOffset如果为null则设为0,存入topicQueueTable
 * <p>
 * 14.调用MessageSysFlag.getTransactionValue(msgInner.getSysFlag())获取tranType来判断该消息是否是事务消息,
 * 如果是TransactionPreparedType或者TransactionRollbackType,则queueOffset=0,这2种类型的消息是不会被消费的见16,17
 * <p>
 * 15.调用byteBuffer.put(this.msgStoreItemMemory.array(), 0, msgLen)写入文件
 * <p>
 * 16.构造DispatchRequest,然后DispatchMessageService.putRequest(dispatchRequest),异步DispatchMessageService.doDispatch(),
 * 分发消息位置信息到ConsumeQueue如果是TransactionPreparedType或者TransactionRollbackType,则不处理,
 * 如果是TransactionNotType或者TransactionCommitType,则调用DefaultMessageStore.this.putMessagePostionInfo()
 * <p>
 * 17.调用ConsumeQueue.putMessagePostionInfo(),20个字节大小的buffer在内存里,offset即消息对应的在CommitLog的offset,
 * size即消息在CommitLog存储中的大小,tagsCode即计算出来的长整数,写入buffer,this.mapedFileQueue.getLastMapedFile(expectLogicOffset)
 * 获取mapedFile,最后mapedFile.appendMessage(this.byteBufferIndex.array())写入文件,作逻辑队列持久化
 * <p>
 * 说明：当Broker接收到从Consumer发来的拉取消息的请求时,根据请求的Topic和queueId获取对应的ConsumerQueue,
 * 由于消息的类型是预备消息或者回滚消息时,不作持久化（即没有把消息体本身存储在CommitLog中的offset保存到ConsumerQueue中）,
 * 那么自然也找不到该消息的逻辑存储单元（也就是前面的20个字节,根据这20个字节可以在CommitLog中定位到一条消息）,最终Consumer也取不到该消息
 * <p>
 * 打个比喻,CommitLog是书的正文,消息体存在于CommitLog中,相当于是书正文中的一个章节,那么ConsumerQueue就是书的目录,记录着章节和页数的对应关系,
 * 如果是预备类型或者回滚类型的章节,目录中没有记录,即使在书的正文中存在,但是我们查找章节时都是通过目录来查找的,目录里没有,就找不到该章节
 * <p>
 * 18.DefaultMessageStore.this.indexService.putRequest(this.requestsRead.toArray()),新建索引
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * 如何在集群消费时保证消费的有序呢？
 * 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 * 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 * consumer收到后,设置是否锁住标志位
 * 这里注意2个变量：
 * consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 * broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>
 * <p>
 * 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 * 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费
 * <p>
 * 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 * 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 * 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>
 * <p>
 * 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 * 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 * 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费
 * <p>
 * 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 * 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 * ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 * this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker
 * <p>
 * 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 * 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 * 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 * 所以,还是把autoCommit设置为true吧
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * 如何在集群消费时保证消费的有序呢？
 * 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 * 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 * consumer收到后,设置是否锁住标志位
 * 这里注意2个变量：
 * consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 * broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>
 * <p>
 * 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 * 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费
 * <p>
 * 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 * 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 * 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>
 * <p>
 * 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 * 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 * 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费
 * <p>
 * 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 * 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 * ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 * this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker
 * <p>
 * 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 * 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 * 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 * 所以,还是把autoCommit设置为true吧
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * 如何在集群消费时保证消费的有序呢？
 * 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 * 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 * consumer收到后,设置是否锁住标志位
 * 这里注意2个变量：
 * consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 * broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>
 * <p>
 * 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 * 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费
 * <p>
 * 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 * 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 * 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>
 * <p>
 * 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 * 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 * 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费
 * <p>
 * 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 * 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 * ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 * this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker
 * <p>
 * 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 * 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 * 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 * 所以,还是把autoCommit设置为true吧
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * 如何在集群消费时保证消费的有序呢？
 * 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 * 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 * consumer收到后,设置是否锁住标志位
 * 这里注意2个变量：
 * consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 * broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>
 * <p>
 * 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 * 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费
 * <p>
 * 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 * 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 * 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>
 * <p>
 * 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 * 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 * 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费
 * <p>
 * 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 * 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 * ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 * this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker
 * <p>
 * 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 * 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 * 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 * 所以,还是把autoCommit设置为true吧
 * <p>
 * rocketmq的顺序消息需要满足2点：
 * 1.Producer端保证发送消息有序,且发送到同一个队列
 * 2.consumer端保证消费同一个队列
 * <p>
 * 如何在集群消费时保证消费的有序呢？
 * 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 * 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 * consumer收到后,设置是否锁住标志位
 * 这里注意2个变量：
 * consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 * broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>
 * <p>
 * 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 * 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费
 * <p>
 * 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 * 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 * 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>
 * <p>
 * 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 * 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 * 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费
 * <p>
 * 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 * 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 * ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 * this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker
 * <p>
 * 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 * 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 * 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 * 所以,还是把autoCommit设置为true吧
 */

/**
 * rocketmq的顺序消息需要满足2点：
 1.Producer端保证发送消息有序,且发送到同一个队列
 2.consumer端保证消费同一个队列
 */

/**
 * 如何在集群消费时保证消费的有序呢？
 1.ConsumeMessageOrderlyService类的start()方法,如果是集群消费,则启动定时任务,定时向broker发送批量锁住当前正在消费的队列集合的消息,
 具体是consumer端拿到正在消费的队列集合,发送锁住队列的消息至broker,broker端返回锁住成功的队列集合
 consumer收到后,设置是否锁住标志位
 这里注意2个变量：
 consumer端的RebalanceImpl里的ConcurrentHashMap processQueueTable,是否锁住设置在ProcessQueue里
 broker端的RebalanceLockManager里的ConcurrentHashMap> mqLockTable,这里维护着全局队列锁,>

 2.ConsumeMessageOrderlyService.ConsumeRequest的run方法是消费消息,这里还有个MessageQueueLock messageQueueLock,
 维护当前consumer端的本地队列锁保证当前只有一个线程能够进行消费

 3.拉到消息存入ProcessQueue,然后判断,本地是否获得锁,全局队列是否被锁住,然后从ProcessQueue里取出消息,用MessageListenerOrderly进行消费
 拉到消息后调用ProcessQueue.putMessage(final List msgs) 存入,具体是存入TreeMapmsgTreeMap
 然后是调用ProcessQueue.takeMessags(final int batchSize)消费,具体是把msgTreeMap里消费过的消息,转移到TreeMap msgTreeMapTemp,>,>

 4.本地消费的事务控制,ConsumeOrderlyStatus.SUCCESS（提交）,ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT（挂起一会再消费）,
 在此之前还有一个变量ConsumeOrderlyContext context的setAutoCommit()是否自动提交
 当SUSPEND_CURRENT_QUEUE_A_MOMENT时,autoCommit设置为true或者false没有区别,本质跟消费相反,把消息从msgTreeMapTemp转移回msgTreeMap,等待下次消费

 当SUCCESS时,autoCommit设置为true时比设置为false多做了2个动作,consumeRequest.getProcessQueue().commit()
 和this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), commitOffset, false);
 ProcessQueue.commit() ：本质是删除msgTreeMapTemp里的消息,msgTreeMapTemp里的消息在上面消费时从msgTreeMap转移过来的
 this.defaultMQPushConsumerImpl.getOffsetStore().updateOffset() ：本质是把拉消息的偏移量更新到本地,然后定时更新到broker

 那么少了这2个动作会怎么样呢,随着消息的消费进行,msgTreeMapTemp里的消息堆积越来越多,
 消费消息的偏移量一直没有更新到broker导致consumer每次重新启动后都要从头开始重复消费
 就算更新了offset到broker,那么msgTreeMapTemp里的消息堆积呢？不知道这算不算bug
 所以,还是把autoCommit设置为true吧
 */

/**
 * 测试git merge
 */
public class Producer {
    public static void main(String[] args) throws MQClientException, InterruptedException {
        DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        for (int i = 0; i < 1000; i++) {
            try {
                Message msg = new Message("Shuihui_TopicTest",// topic
                        "TagA",// tag
                        ("Hello RocketMQ 个动作会怎么样呢" + i).getBytes(RemotingHelper.DEFAULT_CHARSET)// body
                );
                SendResult sendResult = producer.send(msg);
                LocalTransactionExecuter tranExecuter = new LocalTransactionExecuter() {
                    @Override
                    public LocalTransactionState executeLocalTransactionBranch(Message msg, Object arg) {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };

                //RocketMQ通过MessageQueueSelector中实现的算法来确定消息发送到哪一个队列上
                //默认提供了领证MessageQueueSelector实现:随机 hash
                final int useId = 3399;
                SendResult sr = producer.send(msg, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                        Integer id = (Integer) arg;
                        int idx = id % mqs.size();
                        return mqs.get(idx);
                    }
                }, useId);

                //producer.sendMessageInTransaction(msg, tranExecuter, arg)
                System.out.println(sendResult);
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(1000);
            }
        }

        producer.shutdown();
    }
}
