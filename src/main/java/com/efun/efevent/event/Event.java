package com.efun.efevent.event;

import java.util.Date;

import org.springframework.context.ApplicationEvent;

import com.alibaba.fastjson.JSONObject;

/**
 * 事件
 * 
 * @author Ken
 *
 */
public class Event extends ApplicationEvent {

	private String eventCode;

	private JSONObject data;

	private Integer times;

	private Date createdTime;

	private Date lastUpdateTime;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Event(Object source) {
		super(source);
	}

	/**
	 * @return the eventCode
	 */
	public String getEventCode() {
		return eventCode;
	}

	/**
	 * @param eventCode
	 *            the eventCode to set
	 */
	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	/**
	 * @return the data
	 */
	public JSONObject getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(JSONObject data) {
		this.data = data;
	}

	/**
	 * @return the times
	 */
	public Integer getTimes() {
		return times;
	}

	/**
	 * @param times
	 *            the times to set
	 */
	public void setTimes(Integer times) {
		this.times = times;
	}

	/**
	 * @return the createdTime
	 */
	public Date getCreatedTime() {
		return createdTime;
	}

	/**
	 * @param createdTime
	 *            the createdTime to set
	 */
	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	/**
	 * @return the lastUpdateTime
	 */
	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	/**
	 * @param lastUpdateTime
	 *            the lastUpdateTime to set
	 */
	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Event [eventCode=" + eventCode + ", data=" + data + ", times="
				+ times + ", createdTime=" + createdTime + ", lastUpdateTime="
				+ lastUpdateTime + "]";
	}

}
