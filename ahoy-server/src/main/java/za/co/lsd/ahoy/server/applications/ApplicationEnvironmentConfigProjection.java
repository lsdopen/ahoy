package za.co.lsd.ahoy.server.applications;

import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.util.Map;

@Projection(name = "applicationEnvironmentConfig", types = {ApplicationEnvironmentConfig.class})
public interface ApplicationEnvironmentConfigProjection {
	ApplicationDeploymentId getId();

	String getRouteHostname();

	Integer getRouteTargetPort();

	Map<String, String> getEnvironmentVariables();

	String getConfigFileName();

	String getConfigFileContent();

	EnvironmentRelease getEnvironmentRelease();

	ReleaseVersion getReleaseVersion();

	ApplicationVersion getApplicationVersion();
}
