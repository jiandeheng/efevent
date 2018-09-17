package com.efun.efevent.event;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.efun.efevent.event.worker.EventHandlerScanner;

@Component
public class EventDistributer implements ApplicationListener<Event> {

	private static List<Class> eventHandlerClasses = new ArrayList<Class>();

	@Async
	@Override
	public void onApplicationEvent(Event event) {
		System.out.println("start onApplicationEvent");
		if (EventDistributer.eventHandlerClasses.isEmpty()) {
			initEventHandlerClasses();
		}
		for (Class clazz : eventHandlerClasses) {
			if (EventHandler.class.isAssignableFrom(clazz)) {
				try {
					Object object = clazz.newInstance();
					if (object instanceof EventHandler) {
						EventHandler eventHandler = (EventHandler) object;
						String eventCode = eventHandler.bindEvent();
						System.out.println("## event.eventCode = "
								+ event.getEventCode()
								+ ", eventHandler.eventCode = "
								+ eventHandler.bindEvent());
						if (eventCode != null
								&& eventCode.equalsIgnoreCase(event
										.getEventCode())) {
							eventHandler.run(event);
						}
					}
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 初始化事件处理器类集合
	 * 
	 */
	private void initEventHandlerClasses() {
		EventHandlerScanner eventHandlerScanner = new EventHandlerScanner();
		List<Class> classes = eventHandlerScanner.listAllEventHandlerClass();
		this.eventHandlerClasses = classes;
	}

}
