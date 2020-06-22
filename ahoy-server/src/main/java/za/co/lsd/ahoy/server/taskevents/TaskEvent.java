package za.co.lsd.ahoy.server.taskevents;

import lombok.Data;
import za.co.lsd.ahoy.server.ReleaseStatusChangedEvent;

@Data
public class TaskEvent {
	private ReleaseStatusChangedEvent releaseStatusChangedEvent;

	public TaskEvent(ReleaseStatusChangedEvent releaseStatusChangedEvent) {
		this.releaseStatusChangedEvent = releaseStatusChangedEvent;
	}
}
