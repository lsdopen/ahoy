package za.co.lsd.ahoy.server.applications;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "applicationEnvironmentConfigLean", types = {ApplicationEnvironmentConfig.class})
public interface ApplicationEnvironmentConfigLeanProjection {
	ApplicationDeploymentId getId();

	String getRouteHostname();
}
