package com.efun.efevent.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.efun.efevent.event.worker.EventHandlerScanner;

/**
 * 事件分发者
 * 
 * @author Ken
 *
 */
@Component
public class EventDistributer implements ApplicationListener<Event> {

	private static List<Class> eventHandlerClasses = new ArrayList<Class>();

	private static DefaultMQPushConsumer producer = null;

	/**
	 * 事件处理器实例map（key：事件标识, value：绑定的事件处理器集合）
	 */
	private static Map<String, List<EventHandler>> eventHandlers = new HashMap<>();

	private void startConsumer() {
		producer = new DefaultMQPushConsumer();

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
	@PostConstruct
	private void initEventHandlers() {
		// 扫描事件处理器文件，得到所有事件处理器的class集合
		EventHandlerScanner eventHandlerScanner = new EventHandlerScanner();
		List<Class> classes = eventHandlerScanner.listAllEventHandlerClass();
		this.eventHandlerClasses = classes;
		// 实例化所有事件处理器,并与事件标识搭建关系
		List<EventHandler> list = new ArrayList<>();
		try {
			for (Class clazz : this.eventHandlerClasses) {
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

}
