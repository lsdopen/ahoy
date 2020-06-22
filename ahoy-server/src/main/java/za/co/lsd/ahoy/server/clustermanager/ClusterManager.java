package za.co.lsd.ahoy.server.clustermanager;

import za.co.lsd.ahoy.server.environments.Environment;

public interface ClusterManager {

	void createEnvironment(Environment environment);

	void deleteEnvironment(Environment environment);
}
