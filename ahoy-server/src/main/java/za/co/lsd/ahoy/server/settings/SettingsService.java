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

package za.co.lsd.ahoy.server.settings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.argocd.ArgoSettingsChangedEvent;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;

import java.util.Optional;

@Service
@Slf4j
public class SettingsService {
	private final SettingsRepository settingsRepository;
	private ApplicationEventPublisher applicationEventPublisher;

	public SettingsService(SettingsRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	@Autowired
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void saveGitSettings(GitSettings gitSettings) {
		settingsRepository.save(new Settings(gitSettings));
	}

	public Optional<GitSettings> getGitSettings() {
		return settingsRepository.findById(Settings.Type.GIT)
			.map(settings -> (GitSettings) settings.getSettings());
	}

	public boolean gitSettingsExists() {
		return settingsRepository.existsById(Settings.Type.GIT);
	}

	public void saveArgoSettings(ArgoSettings argoSettings) {
		settingsRepository.save(new Settings(argoSettings));
		applicationEventPublisher.publishEvent(new ArgoSettingsChangedEvent(this, argoSettings));
	}

	public Optional<ArgoSettings> getArgoSettings() {
		return settingsRepository.findById(Settings.Type.ARGO)
			.map(settings -> (ArgoSettings) settings.getSettings());
	}

	public boolean argoSettingsExists() {
		return settingsRepository.existsById(Settings.Type.ARGO);
	}

	public void saveDockerSettings(DockerSettings dockerSettings) {
		settingsRepository.save(new Settings(dockerSettings));
	}

	public Optional<DockerSettings> getDockerSettings() {
		return settingsRepository.findById(Settings.Type.DOCKER)
			.map(settings -> (DockerSettings) settings.getSettings());
	}
}
