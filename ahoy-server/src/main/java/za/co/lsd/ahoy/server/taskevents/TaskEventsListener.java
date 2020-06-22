package za.co.lsd.ahoy.server.taskevents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.ReleaseStatusChangedEvent;

@Component
@Slf4j
public class TaskEventsListener {
	private final TaskEventsService taskEventsService;

	public TaskEventsListener(TaskEventsService taskEventsService) {
		this.taskEventsService = taskEventsService;
	}

	@EventListener
	public void onReleaseStatusChanged(ReleaseStatusChangedEvent releaseStatusChangedEvent) {
		TaskEvent taskEvent = new TaskEvent(releaseStatusChangedEvent);
		log.debug("Environment release status changed, sending event: {}", taskEvent);
		taskEventsService.sendTaskEvent(taskEvent);
	}
}
