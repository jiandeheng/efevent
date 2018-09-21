package com.efun.efevent.event;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

/**
 * 事件管理器
 * 
 * @author Ken
 *
 */
@Component
public class EventManager {

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

	private static DefaultMQProducer producer = null;

	private static final String PRODUCER_GROUP = "EventManager";
	private static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";

	/**
	 * redis事件队列key前缀
	 */
	private static final String EVENT_REDIS_KEY_PREFIX = "efevent:";

	/**
	 * MQ开关
	 */
	private static final boolean MQ_SWITCH = true;

	/**
	 * redis开关
	 */
	private static final boolean REDIS_SWITCH = true;

	/**
	 * 事件处理器实例map（key：事件标识, value：绑定的事件处理器集合）
	 */
	private static Map<String, List<EventHandler>> eventHandlers = new HashMap<>();

	/**
	 * 发布事件
	 * 
	 * @param event
	 * @return
	 */
	public void publish(Event event) {
		publish(event, false);
	}

	/**
	 * 发布事件
	 * 
	 * @param event
	 * @param isPublishToMiddleware
	 */
	public void publish(Event event, boolean isPublishToMiddleware) {
		publishToApplication(event);
		// 发布到中间件
		if (isPublishToMiddleware) {
			publishToMQ(event);
			publishToRedis(event);
		}
	}

	/**
	 * 不发到MQ
	 * 
	 * @param event
	 */
	private void publishToMQ(Event event) {
		if (!MQ_SWITCH) {
			return;
		}
		try {
			if (producer == null) {
				System.out.println("生产者未启动f");
			}
			sendMessage(event);
		} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发送MQ信息
	 * 
	 * @param event
	 * @throws MQClientException
	 * @throws RemotingException
	 * @throws MQBrokerException
	 * @throws InterruptedException
	 * @throws UnsupportedEncodingException
	 */
	private void sendMessage(Event event) throws MQClientException, RemotingException, MQBrokerException,
			InterruptedException, UnsupportedEncodingException {
		Message message = new Message();
		message.setTopic(event.getEventCode());
		message.setBody((JSON.toJSONBytes(event, JSON.DEFAULT_GENERATE_FEATURE)));
		SendResult sendResult = producer.send(message);
		System.out.println("# send MQ Message, result = " + sendResult);
	}

	/**
	 * 启动生产者
	 * 
	 * @throws MQClientException
	 * 
	 */
	private void startProducer() throws MQClientException {
		if (!MQ_SWITCH) {
			return;
		}
		System.out.println("## startProducer ... ");
		producer = new DefaultMQProducer();
		producer.setProducerGroup(PRODUCER_GROUP);
		producer.setNamesrvAddr(NAME_SERVER_ADDRESS);
		producer.start();
	}

	/**
	 * 发布到redis
	 * 
	 * @param event
	 */
	private void publishToRedis(Event event) {
		if (!REDIS_SWITCH) {
			return;
		}
		String key = getRedisEventQueueCacheKey(event.getEventCode());
		// Long result = redisTemplate.opsForList().leftPush(key, JSON.toJSON(event));
		Long result = redisTemplate.opsForList().leftPush(key, event);
		System.out.println(
				"## publish event to redis queue, result = " + result + " key = " + key + ", event = " + event);
	}

	/**
	 * 获取事件队列key
	 * 
	 * @param eventCode
	 * @return
	 */
	public static String getRedisEventQueueCacheKey(String eventCode) {
		StringBuilder cacheKey = new StringBuilder(EVENT_REDIS_KEY_PREFIX);
		cacheKey.append(eventCode);
		return cacheKey.toString();
	}

	/**
	 * 应用内发布事件
	 * 
	 * @param event
	 */
	private void publishToApplication(Event event) {
		applicationEventPublisher.publishEvent(event);
		System.out.println("start publishToApplication");
	}

	@PostConstruct
	private void initEventManager() {
		System.out.println("## start initEventManager ...");
		try {
			startProducer();
		} catch (MQClientException e) {
			System.out.println("start mq producer exception , e = " + e);
		}
	}

}
