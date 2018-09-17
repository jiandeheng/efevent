package com.efun.efevent.event.handle;

import com.efun.efevent.event.Event;
import com.efun.efevent.event.EventHandler;

/**
 * 登录事件处理器
 * 
 * @author Ken
 *
 */
public class LoginEventHandler implements EventHandler {

	@Override
	public void handle(Event event) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("LoginEventHandler doing, event = " + event);
	}

	@Override
	public String bindEvent() {
		return "login_event";
	}

}
