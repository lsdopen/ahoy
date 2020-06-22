package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.environments.EnvironmentProjection;
import za.co.lsd.ahoy.server.releases.ReleaseProjection;
import za.co.lsd.ahoy.server.releases.ReleaseVersionProjection;

@Projection(name = "environmentRelease", types = {EnvironmentRelease.class})
public interface EnvironmentReleaseProjection {

	EnvironmentReleaseId getId();

	ReleaseProjection getRelease();

	EnvironmentProjection getEnvironment();

	ReleaseVersionProjection getCurrentReleaseVersion();

	ReleaseVersionProjection getPreviousReleaseVersion();

	@Value("#{target.latestReleaseVersion()}")
	ReleaseVersionProjection getLatestReleaseVersion();

	@Value("#{target.hasCurrentReleaseVersion()}")
	Boolean getDeployed();

	HealthStatus.StatusCode getStatus();

	Integer getApplicationsReady();
}
