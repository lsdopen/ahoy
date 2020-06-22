package za.co.lsd.ahoy.server.releases;

import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RepositoryEventHandler
public class ReleaseVersionApplicationVersionLinkHandler {

	@HandleBeforeLinkSave
	public void handleBeforeLink(ReleaseVersion releaseVersion, Collection<ApplicationVersion> applicationVersions) {
		Set<Long> uniques = new HashSet<>();
		Optional<Long> duplicates = applicationVersions.stream().map(applicationVersion -> applicationVersion.getApplication().getId())
			.filter(e -> !uniques.add(e))
			.findAny();
		if (duplicates.isPresent()) {
			throw new IllegalArgumentException("Cannot link duplicate applications to a single release version");
		}
	}
}
