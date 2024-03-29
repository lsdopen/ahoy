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

package za.co.lsd.ahoy.server.docker;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.settings.SettingsService;

import java.util.Optional;

@Component
public class DockerRegistryProvider {
	private final SettingsService settingsService;

	public DockerRegistryProvider(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public Optional<DockerRegistry> dockerRegistryFor(String name) {
		return settingsService.getDockerSettings().stream()
			.flatMap(dockerSettings -> dockerSettings.getDockerRegistries().stream())
			.filter(dockerRegistry -> dockerRegistry.getName().equals(name))
			.findFirst();
	}
}
