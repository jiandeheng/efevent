package com.efun.efevent.event;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * 事件补偿器
 * 
 * @author Ken
 *
 */
@Component
public class EventCompensator {

	@Autowired
	EventManager eventManager;

	/**
	 * 记录异常事件的文件
	 */
	public static final String FILE_PATH = "C:\\file\\event\\event.log";

	private static BufferedWriter writer;

	private static BufferedReader reader;

	/**
	 * 事件重发最大次数
	 */
	private static final int RESEND_TIMES_MAX = 3;

	/**
	 * 事件重新发布周期
	 */
	private static final long EVENT_RESEND_PERIOD = 10 * 1000;

	/**
	 * 等待追加到文件的事件阻塞队列
	 */
	public static BlockingQueue<Event> blockingQueue = new LinkedBlockingQueue<Event>();

	public static Object lockObject = new Object();

	/**
	 * 初始化事件补偿器
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	private void init() throws IOException {
		initWriterAndReader();
		startEventFileAppendWorker();
	}

	private void startEventFileAppendWorker() {
		Thread thread = new Thread(new EventFileAppendWorker());
		thread.setName("EventFileAppendWorker");
		thread.start();
	}

	/**
	 * 初始化文件writer和reader
	 * 
	 * @throws IOException
	 */
	private void initWriterAndReader() throws IOException {
		File file = new File(FILE_PATH);
		FileWriter out = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(out);
		FileReader in = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(in);
		writer = bufferedWriter;
		reader = bufferedReader;
	}

	/**
	 * 销毁前执行
	 * 
	 * @throws IOException
	 */
	@PreDestroy
	private void dostory() throws IOException {
		writer.close();
		reader.close();
	}

	/**
	 * 异常事件追加到文件，等待重发
	 */
	public static void waitToResend(Event event) {
		event.setLastUpdateTime(new Date());
		event.setTimes(event.getTimes() + 1);
		blockingQueue.offer(event);
	}

	/**
	 * 重发事件（定时任务）
	 * 
	 * @throws IOException
	 */
	@Scheduled(cron = "0,30 * * * * ?")
	private void resend() throws IOException {
		// 加锁
		synchronized (lockObject) {
			BufferedReader bufferedReader = null;
			BufferedWriter bufferedWriter = null;
			Stream<String> lines = null;
			try {
				System.out.println("# start event Compensator");
				File file = new File(FILE_PATH);
				FileReader in = new FileReader(file);
				bufferedReader = new BufferedReader(in);
				lines = bufferedReader.lines();
				Iterator<String> it = lines.iterator();
				List<Event> events = new LinkedList<Event>();
				while (it.hasNext()) {
					String jsonString = it.next();
					System.out.println(jsonString);
					Event event = JSONObject.parseObject(jsonString,
							Event.class);
					// 未满足条件的等待下一次检查
					if (event.getTimes() <= RESEND_TIMES_MAX
							&& (System.currentTimeMillis() - event
									.getLastUpdateTime().getTime()) < EVENT_RESEND_PERIOD) {
						events.add(event);
						continue;
					}
					// 超过重试次数的，丢弃
					if (event.getTimes() > RESEND_TIMES_MAX) {
						continue;
					}
					// 满足条件的重新发布
					eventManager.publish(event);
					System.out.println("# 重新发布事件, event = " + event);
				}
				// 将未满足条件的event放回到文件中
				FileWriter out = new FileWriter(file);
				bufferedWriter = new BufferedWriter(out);
				bufferedWriter.write("");
				for (Event event : events) {
					bufferedWriter.append(JSONObject.toJSONString(event));
					bufferedWriter.newLine();
				}
				bufferedWriter.flush();
			} finally {
				if (lines != null) {
					lines.close();
				}
				if (bufferedReader != null) {
					bufferedReader.close();
				}
				if (bufferedWriter != null) {
					bufferedWriter.close();
				}
			}
		}
	}
}
