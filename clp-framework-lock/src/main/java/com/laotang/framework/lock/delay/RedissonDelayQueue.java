package com.laotang.framework.lock.delay;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于 Redisson 的延时队列。
 *
 * <p>原理:消息先进入 ZSet(score = 到期时间),Redisson 的定时任务把到期消息
 * 转移到 BlockingQueue,消费者从 BlockingQueue 阻塞获取。</p>
 *
 * <p><b>可靠性:</b>底层是 Redis,Redis 丢数据会丢消息。
 * 业务上必须配一个定时扫表的兜底任务。</p>
 */
@Component
@RequiredArgsConstructor
public class RedissonDelayQueue {

    private static final Logger log = LoggerFactory.getLogger(RedissonDelayQueue.class);

    private final RedissonClient redisson;
    private final Map<String, Thread> consumers = new ConcurrentHashMap<>();

    /**
     * 投递一条延时消息。
     */
    public void offer(String queueName, String payload, long delay, TimeUnit unit) {
        RBlockingQueue<String> blockingQueue = redisson.getBlockingQueue(queueName, StringCodec.INSTANCE);
        RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(blockingQueue);
        delayedQueue.offer(payload, delay, unit);
    }

    /**
     * 订阅队列。同一个队列只启动一个消费线程。
     */
    public void subscribe(String queueName, Consumer<String> handler) {
        consumers.computeIfAbsent(queueName, name -> {
            RBlockingQueue<String> blockingQueue = redisson.getBlockingQueue(name, StringCodec.INSTANCE);
            // 必须获取一次,Redisson 才会启动"到期转移"的定时任务
            redisson.getDelayedQueue(blockingQueue);

            Thread thread = new Thread(() -> consume(name, blockingQueue, handler), "delay-" + name);
            thread.setDaemon(true);
            thread.start();
            return thread;
        });
    }

    private void consume(String queueName, RBlockingQueue<String> queue, Consumer<String> handler) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String payload = queue.take();
                handler.accept(payload);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("延时队列消费失败 queue={}", queueName, e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        consumers.values().forEach(Thread::interrupt);
        consumers.clear();
    }
}
