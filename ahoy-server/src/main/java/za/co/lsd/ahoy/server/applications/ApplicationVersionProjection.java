package za.co.lsd.ahoy.server.applications;

import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.docker.DockerRegistry;

import java.util.List;
import java.util.Map;

@Projection(name = "applicationVersion", types = {ApplicationVersion.class})
public interface ApplicationVersionProjection {
	long getId();

	DockerRegistry getDockerRegistry();

	String getImage();

	String getVersion();

	List<Integer> getServicePorts();

	Map<String, String> getEnvironmentVariables();

	String getHealthEndpointPath();

	Integer getHealthEndpointPort();

	String getHealthEndpointScheme();

	List<ApplicationConfig> getConfigs();

	String getConfigPath();

	Application getApplication();
}
