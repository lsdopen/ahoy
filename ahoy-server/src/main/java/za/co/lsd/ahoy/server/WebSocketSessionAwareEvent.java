package za.co.lsd.ahoy.server;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WebSocketSessionAwareEvent extends ApplicationEvent {
	private final String webSocketSessionId;

	public WebSocketSessionAwareEvent(Object source, String webSocketSessionId) {
		super(source);
		this.webSocketSessionId = webSocketSessionId;
	}
}
