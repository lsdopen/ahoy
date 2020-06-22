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

package za.co.lsd.ahoy.server.settings;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.argocd.ArgoSettingsRepository;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.docker.DockerSettingsRepository;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.GitSettingsRepository;

@Component
public class DbSettingsProvider implements SettingsProvider {
	private final GitSettingsRepository gitSettingsRepository;
	private final ArgoSettingsRepository argoSettingsRepository;
	private final DockerSettingsRepository dockerSettingsRepository;

	public DbSettingsProvider(GitSettingsRepository gitSettingsRepository,
	                          ArgoSettingsRepository argoSettingsRepository,
	                          DockerSettingsRepository dockerSettingsRepository) {
		this.gitSettingsRepository = gitSettingsRepository;
		this.argoSettingsRepository = argoSettingsRepository;
		this.dockerSettingsRepository = dockerSettingsRepository;
	}

	@Override
	public GitSettings getGitSettings() {
		return gitSettingsRepository.findById(1L).orElse(new GitSettings());
	}

	@Override
	public ArgoSettings getArgoSettings() {
		return argoSettingsRepository.findById(1L).orElse(new ArgoSettings());
	}

	@Override
	public DockerSettings getDockerSettings() {
		return dockerSettingsRepository.findById(1L).orElse(new DockerSettings());
	}
}
