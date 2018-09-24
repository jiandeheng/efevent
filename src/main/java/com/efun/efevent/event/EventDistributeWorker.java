package com.efun.efevent.event;

/**
 * 事件分发worker
 * 
 * @author Ken
 *
 */
public class EventDistributeWorker implements Runnable {

	private EventHandler eventHandler;

	private Event event;

	public EventDistributeWorker(EventHandler eventHandler, Event event) {
		this.eventHandler = eventHandler;
		this.event = event;
	}

	@Override
	public void run() {
		if (this.eventHandler != null && this.event != null) {
			eventHandler.run(event);
			return;
		}
		System.out.println("## eventHandler or event == null, eventHandler = "
				+ eventHandler + ", event = " + event);
	}

}
