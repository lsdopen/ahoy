/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TestTaskProgressListener {
	private Map<String, List<TaskProgressEvent>> eventsMap = new Hashtable<>();

	@EventListener
	public void onTaskProgressEvent(TaskProgressEvent event) {
		if (eventsMap.containsKey(event.getId())) {
			eventsMap.get(event.getId()).add(event);
		} else {
			eventsMap.put(event.getId(), new ArrayList<>(List.of(event)));
		}
	}

	public List<TaskProgressEvent> getEvents(String id) {
		if (eventsMap.containsKey(id))
			return eventsMap.get(id);

		return List.of();
	}
}
