/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
