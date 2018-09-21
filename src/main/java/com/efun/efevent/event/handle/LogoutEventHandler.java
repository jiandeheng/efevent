package com.efun.efevent.event.handle;

import com.efun.efevent.event.Event;
import com.efun.efevent.event.EventHandler;

/**
 * 登出事件处理器
 * 
 * @author Ken
 *
 */
public class LogoutEventHandler implements EventHandler {

	/**
	 * 事件处理逻辑
	 */
	@Override
	public void handle(Event event) {
		try {
			System.out.println("LogoutEventHandler start ..., event = " + event);
			Thread.sleep(5000);
			System.out.println("LogoutEventHandler end ... ");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 返回绑定的事件标识
	 */
	@Override
	public String bindEvent() {
		return "logout_event";
	}

}
