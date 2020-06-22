package za.co.lsd.ahoy.server.clustermanager;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environments.Environment;

@Component
@Scope("prototype")
public class NoOpClusterManager implements ClusterManager {

	public NoOpClusterManager(Cluster cluster) {
	}

	@Override
	public void createEnvironment(Environment environment) {
		sleep(3000);
	}

	@Override
	public void deleteEnvironment(Environment environment) {
		sleep(2000);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
