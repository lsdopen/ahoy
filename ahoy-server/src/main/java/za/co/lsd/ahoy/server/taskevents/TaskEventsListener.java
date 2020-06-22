/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
