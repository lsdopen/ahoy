package za.co.lsd.ahoy.server.environments;

import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.cluster.Cluster;

@Projection(name = "environment", types = {Environment.class})
public interface EnvironmentProjection {
	long getId();

	String getName();

	Cluster getCluster();
}
