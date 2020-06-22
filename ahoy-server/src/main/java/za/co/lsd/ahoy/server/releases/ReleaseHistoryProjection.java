package za.co.lsd.ahoy.server.releases;

import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseProjection;

import java.time.LocalDateTime;

@Projection(name = "releaseHistory", types = {ReleaseHistory.class})
public interface ReleaseHistoryProjection {
	long getId();

	EnvironmentReleaseProjection getEnvironmentRelease();

	ReleaseVersion getReleaseVersion();

	ReleaseHistoryAction getAction();

	ReleaseHistoryStatus getStatus();

	LocalDateTime getTime();

	String getDescription();
}
