package com.efun.efevent.event;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import com.alibaba.fastjson.JSONObject;

/**
 * 追加事件到文件线程
 * 
 * @author Ken
 *
 */
public class EventFileAppendWorker implements Runnable {

	private static final String FILE_PATH = EventCompensator.FILE_PATH;

	private BufferedWriter writer;

	@Override
	public void run() {
		BlockingQueue<Event> blockingQueue = EventCompensator.blockingQueue;
		while (true) {
			synchronized (EventCompensator.lockObject) {
				try {
					if (blockingQueue.isEmpty()) {
						Thread.sleep(10000);
						continue;
					}
					Event event = blockingQueue.poll();
					if (event != null) {
						BufferedWriter writer = getWriter();
						writer.append(JSONObject.toJSONString(event));
						writer.newLine();
						writer.flush();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private BufferedWriter getWriter() throws IOException {
		if (writer != null) {
			return writer;
		}
		File file = new File(FILE_PATH);
		FileWriter out = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(out);
		writer = bufferedWriter;
		return writer;
	}
}
