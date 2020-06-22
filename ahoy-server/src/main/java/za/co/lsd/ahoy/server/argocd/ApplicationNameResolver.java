package za.co.lsd.ahoy.server.argocd;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;

@Component
public class ApplicationNameResolver {

	public String resolve(EnvironmentRelease environmentRelease) {
		Environment environment = environmentRelease.getEnvironment();
		Cluster cluster = environment.getCluster();
		Release release = environmentRelease.getRelease();
		return cluster.getName() + "-" + environment.getName() + "-" + release.getName();
	}
}
