package za.co.lsd.ahoy.server;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;

@Getter
public class ReleaseStatusChangedEvent extends ApplicationEvent {
	private final EnvironmentReleaseId environmentReleaseId;
	private final Long releaseVersionId;

	public ReleaseStatusChangedEvent(Object source, EnvironmentReleaseId environmentReleaseId, Long releaseVersionId) {
		super(source);
		this.environmentReleaseId = environmentReleaseId;
		this.releaseVersionId = releaseVersionId;
	}
}
