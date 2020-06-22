package za.co.lsd.ahoy.server;

import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;

import java.util.Objects;

public final class ReleaseUtils {

	private ReleaseUtils() {
	}

	public static String resolveReleasePath(EnvironmentRelease environmentRelease) {
		Objects.requireNonNull(environmentRelease, "environmentRelease is required");

		Environment environment = environmentRelease.getEnvironment();
		Release release = environmentRelease.getRelease();
		Cluster cluster = environment.getCluster();

		return cluster.getName() + "/" +
			environment.getName() + "/" +
			release.getName();
	}
}
