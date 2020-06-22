package za.co.lsd.ahoy.server.applications;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.util.Optional;

@Component
public class ApplicationEnvironmentConfigProvider {
	private final ApplicationEnvironmentConfigRepository repository;

	public ApplicationEnvironmentConfigProvider(ApplicationEnvironmentConfigRepository repository) {
		this.repository = repository;
	}

	public Optional<ApplicationEnvironmentConfig> environmentConfigFor(EnvironmentRelease environmentRelease,
	                                                                   ReleaseVersion releaseVersion,
	                                                                   ApplicationVersion applicationVersion) {

		ApplicationDeploymentId id = new ApplicationDeploymentId(
			environmentRelease.getId(),
			releaseVersion.getId(),
			applicationVersion.getId());
		return repository.findById(id);
	}
}
