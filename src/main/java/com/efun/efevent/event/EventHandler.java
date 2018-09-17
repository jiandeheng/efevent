package com.efun.efevent.event;

/**
 * 事件处理器
 * 
 * @author Ken
 *
 */
public interface EventHandler {

	/**
	 * 绑定事件，返回事件标识
	 * 
	 * @return
	 */
	public String bindEvent();

	/**
	 * 事件处理
	 * 
	 * @param event
	 */
	public void handle(Event event);

	/**
	 * 事件处理器默认运行入口
	 */
	default void run(Event event) {
		try {
			handle(event);
		} catch (Exception e) {
			System.out.println("wait to resend event");
		}

	}
}
