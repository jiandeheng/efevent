package com.efun.efevent.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 事件分发者
 * 
 * @author Ken
 *
 */
@Component
public class EventDistributer implements ApplicationListener<Event> {

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

	/**
	 * MQ消费者
	 */
	private static DefaultMQPushConsumer consumer = null;

	/**
	 * MQ开关
	 */
	private static final boolean MQ_SWITCH = true;

	/**
	 * redis开关
	 */
	private static final boolean REDIS_SWITCH = true;

	private static final String CONSUMER_GROUP = "EventDistributer";
	private static final String NAME_SERVER_ADDRESS = "127.0.0.1:9876";

	/**
	 * 事件处理器实例map（key：事件标识, value：绑定的事件处理器集合）
	 */
	private static Map<String, List<EventHandler>> eventHandlers = new HashMap<>();

	/**
	 * 初始化事件分发器
	 */
	@PostConstruct
	private void initEventDistributer() {
		System.out.println("## start initEventDistributer ...");
		// 初始化事件处理器
		initEventHandlers();
		// 启动redis事件队列监听线程
		startRedisEventQueueListenerWorkers();
		// 启动消费者
		startConsumer();
	}

	/**
	 * 启动MQ消费者
	 */
	private void startConsumer() {
		if (!MQ_SWITCH) {
			return;
		}
		System.out.println("## startConsumer ... ");
		try {
			consumer = new DefaultMQPushConsumer();
			consumer.setConsumerGroup(CONSUMER_GROUP);
			consumer.setNamesrvAddr(NAME_SERVER_ADDRESS);
			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
			consumer.setMessageModel(MessageModel.CLUSTERING);
			if (!eventHandlers.isEmpty()) {
				for (String eventCode : eventHandlers.keySet()) {
					consumer.subscribe(eventCode, "*");
					System.out.println("## Event MQ consumer subscribe " + eventCode);
				}
			}
			consumer.registerMessageListener(new MessageListenerConcurrently() {
				@Override
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
						ConsumeConcurrentlyContext context) {
					MessageExt msg = msgs.get(0);
					System.out.println("## receive new MQ message = " + msg);
					String topic = msg.getTopic();
					String tags = msg.getTags();
					JSONObject eventJsonObj = JSON.parseObject(msg.getBody(), JSONObject.class);
					String eventJsonString = eventJsonObj.toJSONString();
					System.out.println("# eventJsonString = " + eventJsonString);
					Event event = null;
					try {
						event = JSONObject.parseObject(msg.getBody(), Event.class);
					} catch (Exception e) {
						System.out.println("## parseObject event exception " + e);
					}
					System.out.println("## event = " + event);
					System.out.println("# receive topic = " + topic + ", tags = " + tags + ", event = " + event);
					distributeEvent(event);
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
			consumer.start();
		} catch (Exception e) {
			System.out.println("startConsumer exception = " + e);
			e.printStackTrace();
		}
	}

	@Async
	@Override
	public void onApplicationEvent(Event event) {
		System.out.println("start onApplicationEvent");
		distributeEvent(event);
	}

	/**
	 * 分发事件给对应的事件处理器
	 * 
	 * @param event
	 */
	private void distributeEvent(Event event) {
		String eventCode = event.getEventCode();
		if (!eventHandlers.containsKey(eventCode)) {
			System.out.println("## 找不到事件处理器处理该事件, eventCode = " + eventCode);
			return;
		}
		for (EventHandler eventHandler : eventHandlers.get(eventCode)) {
			eventHandler.run(event);
		}
	}

	/**
	 * 初始化事件处理器类集合
	 * 
	 */

	private void initEventHandlers() {
		try {
			// 扫描事件处理器文件，得到所有事件处理器的class集合
			EventHandlerScanner eventHandlerScanner = new EventHandlerScanner();
			List<Class> eventHandlerClasses = eventHandlerScanner.listAllEventHandlerClass();
			// 实例化所有事件处理器,并与事件标识搭建关系
			List<EventHandler> list = new ArrayList<>();
			for (Class clazz : eventHandlerClasses) {
				Object object = clazz.newInstance();
				if (object instanceof EventHandler) {
					EventHandler eventHandler = (EventHandler) object;
					String eventCode = eventHandler.bindEvent();
					addToEventHandlers(eventCode, eventHandler);
				}
			}
		} catch (Exception e) {
			System.out.println("## initEventHandlerClasses exception, e = " + e);
		}
	}

	/**
	 * 聚集事件处理器
	 * 
	 * @param eventCode
	 * @param eventHandler
	 */
	private void addToEventHandlers(String eventCode, EventHandler eventHandler) {
		if (!eventHandlers.containsKey(eventCode)) {
			List<EventHandler> list = new ArrayList<>();
			list.add(eventHandler);
			eventHandlers.put(eventCode, list);
			return;
		}
		List<EventHandler> list = eventHandlers.get(eventCode);
		list.add(eventHandler);
	}

	/**
	 * 启动redis事件队列监听线程
	 */
	private void startRedisEventQueueListenerWorkers() {
		if (!REDIS_SWITCH) {
			return;
		}
		// 事件标识集合
		Set<String> eventCodes = eventHandlers.keySet();
		if (eventCodes.isEmpty()) {
			System.out.println("没有事件处理器，不需要监听redis事件队列");
			return;
		}
		// 启动监听redis事件队列线程（一个事件一个队列一个线程监听）
		for (String eventCode : eventCodes) {
			Thread redisEventQueueListenerWorkerThread = new Thread(new RedisEventQueueListenerWorker(eventCode));
			redisEventQueueListenerWorkerThread.setName("redisEventQueueListenerWorkerThread_" + eventCode);
			redisEventQueueListenerWorkerThread.start();
		}
	}

	/**
	 * redis事件队列监听worker
	 * 
	 * @author Ken
	 *
	 */
	class RedisEventQueueListenerWorker implements Runnable {

		/**
		 * 事件标识
		 */
		private String eventCode;

		/**
		 * 线程最大休息轮数
		 */
		private static final int MAX_ROUND = 5;

		/**
		 * 每轮休息的时间（毫秒）
		 */
		private static final long SLEEP_TIME_PER_ROUND = 10 * 1000;

		public RedisEventQueueListenerWorker(String eventCode) {
			this.eventCode = eventCode;
		}

		@Override
		public void run() {
			System.out.println(
					"## redisEventQueueListenerWorkerThread start ... thead = " + Thread.currentThread().getName());
			try {
				// 停止监听轮数，越久没事件来，线程休息越久（会有个阈值）
				int round = 0;
				String eventQueueKey = EventManager.getRedisEventQueueCacheKey(eventCode);
				while (true) {
					// 分发事件
					Object object = redisTemplate.opsForList().rightPop(eventQueueKey);
					if (object != null) {
						System.out.println("## object = " + object);
						// Event event = JSONObject.parseObject(JSONObject.toJSONString(object),
						// Event.class);
						Event event = (Event) object;
						System.out.println("## receive event from redis queue, eventQueueKey = " + eventQueueKey
								+ ", event = " + event);
						// 分发交给事件处理器处理应该是异步的
						distributeEvent(event);
						// 重置轮数
						round = 0;
						continue;
					}
					// 没事件来，休息一会
					System.out.println("# redis事件队列没事件来, eventCode = " + eventCode + ", round = " + round
							+ ", thread = " + Thread.currentThread().getName());
					// round = round >= MAX_ROUND ? MAX_ROUND : (round++);
					if (round >= MAX_ROUND) {
						round = MAX_ROUND;
					} else {
						round++;
					}
					Thread.sleep(round * SLEEP_TIME_PER_ROUND);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
