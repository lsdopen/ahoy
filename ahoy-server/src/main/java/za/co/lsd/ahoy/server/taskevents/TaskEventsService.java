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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.*;

@Service
@Slf4j
public class TaskEventsService {
	private final SimpMessagingTemplate template;

	public TaskEventsService(SimpMessagingTemplate template) {
		this.template = template;
	}

	public void sendTaskEvent(TaskEvent taskEvent) {
		try {
			template.convertAndSend("/events", taskEvent);
		} catch (MessagingException e) {
			log.error("Failed to send task event", e);
		}
	}

	public void sendTaskEvent(TaskEvent taskEvent, String sessionId) {
		try {
			template.convertAndSendToUser(sessionId, "/events", taskEvent, createHeaders(sessionId));
		} catch (MessagingException e) {
			log.error("Failed to send task event", e);
		}
	}

	private MessageHeaders createHeaders(String sessionId) {
		SimpMessageHeaderAccessor headerAccessor = create(SimpMessageType.MESSAGE);
		headerAccessor.setSessionId(sessionId);
		return headerAccessor.getMessageHeaders();
	}
}
