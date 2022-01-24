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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles(profiles = {"test", "keycloak"})
class SettingsServiceTest {
	@Autowired
	private SettingsService settingsService;

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void saveAndGetGitSettings() {
		// given
		GitSettings gitSettings = new GitSettings("git@github.com:lsdopen/ahoy.git");

		// when
		settingsService.saveGitSettings(gitSettings);
		Optional<GitSettings> readGitSettings = settingsService.getGitSettings();

		// then
		assertTrue(readGitSettings.isPresent(), "Git settings should have been saved");
		assertEquals(gitSettings, readGitSettings.get(), "Saved and retrieved git settings should be equal");
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void saveAndGetArgoSettings() {
		// given
		ArgoSettings argoSettings = new ArgoSettings();
		argoSettings.setArgoServer("argo-server");
		argoSettings.setArgoToken("argo-token");

		// when
		settingsService.saveArgoSettings(argoSettings);
		Optional<ArgoSettings> readArgoSettings = settingsService.getArgoSettings();

		// then
		assertTrue(readArgoSettings.isPresent(), "Argo settings should have been saved");
		assertEquals(argoSettings, readArgoSettings.get(), "Saved and retrieved argo settings should be equal");
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void saveAndGetDockerSettings() {
		// given
		DockerSettings dockerSettings = new DockerSettings();
		DockerRegistry dockerRegistry1 = new DockerRegistry("reg1", "reg1-server", "reg1-username", "reg1-password");
		DockerRegistry dockerRegistry2 = new DockerRegistry("reg2", "reg2-server", "reg2-username", "reg2-password");
		dockerSettings.setDockerRegistries(Arrays.asList(dockerRegistry1, dockerRegistry2));

		// when
		settingsService.saveDockerSettings(dockerSettings);
		Optional<DockerSettings> readDockerSettings = settingsService.getDockerSettings();

		// then
		assertTrue(readDockerSettings.isPresent(), "Docker settings should have been saved");
		assertEquals(dockerSettings, readDockerSettings.get(), "Saved and retrieved docker settings should be equal");
	}
}
