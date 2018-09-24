package com.efun.efevent.event;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;

/**
 * 事件工厂类
 * 
 * @author Ken
 *
 */
public class EventFactory {

	/**
	 * 生产事件
	 * 
	 * @param eventCode
	 * @param data
	 * @return
	 */
	public static Event create(String eventCode, Object data) {
		if (data == null) {
			data = new Object();
		}
		Event event = new Event(new Object());
		event.setEventCode(eventCode);
		event.setData(JSONObject.parseObject(JSONObject.toJSONString(data)));
		event.setCreatedTime(new Date());
		event.setLastUpdateTime(new Date());
		event.setTimes(1);
		return event;
	}

	public static String eventCode(EventCode eventCode) {
		return new String(eventCode.toString());
	}

}
