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
