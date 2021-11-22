/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.settings;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;

@Component
public class SettingsProvider {
	private final SettingsService settingsService;

	public SettingsProvider(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public GitSettings getGitSettings() {
		return settingsService.getGitSettings()
			.orElseThrow(() -> new SettingsException("Git settings have not been configured"));
	}

	public ArgoSettings getArgoSettings() {
		return settingsService.getArgoSettings()
			.orElseThrow(() -> new SettingsException("Argo settings have not been configured"));
	}

	public DockerSettings getDockerSettings() {
		return settingsService.getDockerSettings()
			.orElseThrow(() -> new SettingsException("Docker settings have not been configured"));
	}
}
