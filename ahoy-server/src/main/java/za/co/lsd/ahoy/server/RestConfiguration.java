package za.co.lsd.ahoy.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseHistory;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

@Configuration
public class RestConfiguration implements RepositoryRestConfigurer {

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
		config.exposeIdsFor(
			Cluster.class,
			Environment.class,
			EnvironmentRelease.class,
			Release.class,
			ReleaseVersion.class,
			ReleaseHistory.class,
			Application.class,
			ApplicationVersion.class,
			ApplicationConfig.class,
			ApplicationEnvironmentConfig.class,
			ApplicationReleaseStatus.class,
			GitSettings.class,
			ArgoSettings.class,
			DockerSettings.class,
			DockerRegistry.class
		);
	}
}
