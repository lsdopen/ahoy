/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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
