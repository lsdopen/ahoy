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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.argocd.ArgoSettingsService;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.GitSettingsService;

@RestController
@RequestMapping("/api/settings")
@Slf4j
public class SettingsController {
	private final SettingsService settingsService;
	private final GitSettingsService gitSettingsService;
	private final ArgoSettingsService argoSettingsService;

	public SettingsController(SettingsService settingsService,
							  GitSettingsService gitSettingsService,
							  ArgoSettingsService argoSettingsService) {
		this.settingsService = settingsService;
		this.gitSettingsService = gitSettingsService;
		this.argoSettingsService = argoSettingsService;
	}

	@PostMapping("/git")
	@ResponseStatus(value = HttpStatus.OK)
	public void saveGitSettings(@RequestBody GitSettings gitSettings) {
		settingsService.saveGitSettings(gitSettings);
	}

	@GetMapping("/git")
	public GitSettings getGitSettings() {
		return settingsService.getGitSettings()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find git settings"));
	}

	@PostMapping("/git/test")
	@ResponseStatus(value = HttpStatus.OK)
	public void testGitConnection(@RequestBody GitSettings gitSettings) {
		gitSettingsService.testConnection(gitSettings);
	}

	@PostMapping("/argo")
	@ResponseStatus(value = HttpStatus.OK)
	public void saveArgoSettings(@RequestBody ArgoSettings argoSettings) {
		settingsService.saveArgoSettings(argoSettings);
	}

	@GetMapping("/argo")
	public ArgoSettings getArgoSettings() {
		return settingsService.getArgoSettings()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find argo settings"));
	}

	@PostMapping("/argo/test")
	@ResponseStatus(value = HttpStatus.OK)
	public void testArgoConnection(@RequestBody ArgoSettings argoSettings) {
		argoSettingsService.testConnection(argoSettings);
	}

	@PostMapping("/docker")
	@ResponseStatus(value = HttpStatus.OK)
	public void saveDockerSettings(@RequestBody DockerSettings dockerSettings) {
		settingsService.saveDockerSettings(dockerSettings);
	}

	@GetMapping("/docker")
	public DockerSettings getDockerSettings() {
		return settingsService.getDockerSettings()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find docker settings"));
	}
}
