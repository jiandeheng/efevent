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

	/**
	 * 事件处理逻辑
	 */
	@Override
	public void handle(Event event) {
		try {
			System.out.println("LoginEventHandler start ..., event = " + event);
			Thread.sleep(5000);
			if((System.currentTimeMillis() % 2) == 0) {
				throw new RuntimeException("LoginEventHandler handle exception");
			}
			System.out.println("LoginEventHandler end ... ");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 返回绑定的事件标识
	 */
	@Override
	public String bindEvent() {
		return "login_event";
	}

}
