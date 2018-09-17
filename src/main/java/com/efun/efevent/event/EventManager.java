package com.efun.efevent.event;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

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

	private static final String PRODUCER_GROUP = "producerA";
	private static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";

	/**
	 * redis事件队列key前缀
	 */
	private static final String EVENT_REDIS_KEY_PREFIX = "efevent:";

	/**
	 * MQ开关
	 */
	private static final boolean MQ_SWITCH = false;

	/**
	 * redis开关
	 */
	private static final boolean REDIS_SWITCH = true;

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
		} catch (MQClientException | RemotingException | MQBrokerException
				| InterruptedException e) {
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
	 */
	private void sendMessage(Event event) throws MQClientException,
			RemotingException, MQBrokerException, InterruptedException {
		Message message = new Message();
		message.setTopic(event.getEventCode());
		message.setBody(event.getData()
				.getBytes(RemotingHelper.DEFAULT_CHARSET));
		SendResult sendResult = producer.send(message);
		System.out.println("# send Message, result = " + sendResult);
	}

	/**
	 * 启动生产者
	 * 
	 * @throws MQClientException
	 * 
	 */
	@PostConstruct
	private void startProducer() throws MQClientException {
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
		String key = getEventQueueCacheKey(event.getEventCode());
		String jsonString = JSONObject.toJSONString(event);
		redisTemplate.opsForList().leftPush(key,
				JSONObject.parseObject(jsonString));
	}

	/**
	 * 获取事件队列key
	 * 
	 * @param eventCode
	 * @return
	 */
	private String getEventQueueCacheKey(String eventCode) {
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

}
