package com.efun.efevent.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.efun.efevent.event.Event;
import com.efun.efevent.event.EventFactory;
import com.efun.efevent.event.EventManager;

@RestController
public class IndexController {

	@Autowired
	EventManager eventManager;

	@RequestMapping("/index")
	public ResponseEntity<Map<String, Object>> index(String value, boolean isPublishToMiddleware, String eventCode) {
		Map<String, Object> result = new HashMap<String, Object>();
		JSONObject data = new JSONObject();
		data.put("userId", value);
		Event event = EventFactory.create(eventCode, data);
		eventManager.publish(event, isPublishToMiddleware);
		return new ResponseEntity<Map<String, Object>>(result, HttpStatus.OK);
	}

}
