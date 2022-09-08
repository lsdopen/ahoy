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

package za.co.lsd.ahoy.server.release;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import za.co.lsd.ahoy.server.AhoyTestServerApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoEvents;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.releases.resources.ResourceNode;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyTestServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = {"test", "keycloak"})
@Slf4j
public class ReleaseControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private ReleaseService releaseService;

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void deploy() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();
		DeployOptions deployOptions = new DeployOptions(1L, "Please deploy");

		when(releaseService.deploy(eq(environmentReleaseId), eq(deployOptions))).thenReturn(environmentRelease);

		// when
		mvc.perform(post("/api/environmentReleases/2_3/deploy")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(deployOptions)))
			.andDo(print())
			.andExpect(status().isOk());

		// then
		verify(releaseService, timeout(1000).times(1)).deploy(eq(environmentReleaseId), eq(deployOptions));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void deployAsDeveloper() throws Exception {
		// given
		DeployOptions deployOptions = new DeployOptions(1L, "Please deploy");

		// when
		mvc.perform(post("/api/environmentReleases/2_3/deploy")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(deployOptions)))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void undeploy() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();

		when(releaseService.undeploy(eq(environmentReleaseId))).thenReturn(environmentRelease);

		// when
		mvc.perform(post("/api/environmentReleases/2_3/undeploy")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		// then
		verify(releaseService, times(1)).undeploy(eq(environmentReleaseId));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void undeployAsDeveloper() throws Exception {
		// when
		mvc.perform(post("/api/environmentReleases/2_3/undeploy")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void promote() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();
		PromoteOptions promoteOptions = new PromoteOptions(1L, false);

		when(releaseService.promote(eq(environmentReleaseId), eq(promoteOptions))).thenReturn(environmentRelease);

		// when
		mvc.perform(post("/api/environmentReleases/2_3/promote")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(promoteOptions)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id.environmentId").value(2L))
			.andExpect(jsonPath("$.id.releaseId").value(3L));

		// then
		verify(releaseService, times(1)).promote(eq(environmentReleaseId), eq(promoteOptions));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void promoteAsDeveloper() throws Exception {
		// given
		PromoteOptions promoteOptions = new PromoteOptions(1L, false);

		// when
		mvc.perform(post("/api/environmentReleases/2_3/promote")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(promoteOptions)))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void upgrade() throws Exception {
		// given
		UpgradeOptions upgradeOptions = new UpgradeOptions("1.1", false);

		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion();
		upgradedReleaseVersion.setId(2L);
		upgradedReleaseVersion.setVersion("1.1");

		when(releaseService.upgrade(eq(1L), eq(upgradeOptions))).thenReturn(upgradedReleaseVersion);

		// when
		mvc.perform(post("/api/releaseVersions/1/upgrade")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(upgradeOptions)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(2L))
			.andExpect(jsonPath("$.version").value("1.1"));

		// then
		verify(releaseService, times(1)).upgrade(eq(1L), eq(upgradeOptions));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void upgradeAsDeveloper() throws Exception {
		// given
		UpgradeOptions upgradeOptions = new UpgradeOptions("1.1", false);

		// when
		mvc.perform(post("/api/releaseVersions/1/upgrade")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(upgradeOptions)))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void remove() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();

		when(releaseService.remove(eq(environmentReleaseId))).thenReturn(CompletableFuture.completedFuture(environmentRelease));

		// when
		mvc.perform(delete("/api/environmentReleases/2_3/remove")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id.environmentId").value(2L))
			.andExpect(jsonPath("$.id.releaseId").value(3L));

		// then
		verify(releaseService, times(1)).remove(eq(environmentReleaseId));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void removeAsDeveloper() throws Exception {
		// when
		mvc.perform(delete("/api/environmentReleases/2_3/remove")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void copyEnvConfig() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();

		when(releaseService.copyEnvConfig(eq(environmentReleaseId), eq(4L), eq(5L))).thenReturn(environmentRelease);

		// when
		mvc.perform(post("/api/environmentReleases/2_3/copyEnvConfig")
				.queryParam("sourceReleaseVersionId", "4")
				.queryParam("destReleaseVersionId", "5")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id.environmentId").value(2L))
			.andExpect(jsonPath("$.id.releaseId").value(3L));

		// then
		verify(releaseService, times(1)).copyEnvConfig(eq(environmentReleaseId), eq(4L), eq(5L));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void copyEnvConfigAsDeveloper() throws Exception {
		// when
		mvc.perform(post("/api/environmentReleases/2_3/copyEnvConfig")
				.queryParam("sourceReleaseVersionId", "4")
				.queryParam("destReleaseVersionId", "5")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void copyApplicationVersionEnvConfig() throws Exception {
		// when
		mvc.perform(post("/api/releaseVersions/1/copyAppEnvConfig")
				.queryParam("sourceApplicationVersionId", "2")
				.queryParam("destApplicationVersionId", "3")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		// then
		verify(releaseService, times(1)).copyApplicationVersionEnvConfig(eq(1L), eq(2L), eq(3L));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void copyApplicationVersionEnvConfigAsDeveloper() throws Exception {
		// when
		mvc.perform(post("/api/releaseVersions/1/copyAppEnvConfig")
				.queryParam("sourceApplicationVersionId", "2")
				.queryParam("destApplicationVersionId", "3")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.releasemanager})
	void duplicate() throws Exception {
		// given
		DuplicateOptions duplicateOptions = new DuplicateOptions(false, false);
		Release duplicatedRelease = new Release(2L, "release-1-copy");

		when(releaseService.duplicate(eq(1L), eq(2L), eq(duplicateOptions))).thenReturn(duplicatedRelease);

		// when
		mvc.perform(post("/api/releases/duplicate/1/2")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(duplicateOptions)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(2L))
			.andExpect(jsonPath("$.name").value("release-1-copy"));

		// then
		verify(releaseService, times(1)).duplicate(eq(1L), eq(2L), eq(duplicateOptions));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void duplicateAsDeveloper() throws Exception {
		// given
		DuplicateOptions duplicateOptions = new DuplicateOptions(false, false);

		// when
		mvc.perform(post("/api/releases/duplicate/1/2")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(duplicateOptions)))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void getResources() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();
		ResourceNode resourceNode = new ResourceNode("root");

		when(releaseService.getResources(eq(environmentReleaseId))).thenReturn(Optional.of(resourceNode));

		// when
		mvc.perform(get("/api/environmentReleases/2_3/resources")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("root"));

		// then
		verify(releaseService, times(1)).getResources(eq(environmentReleaseId));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.user})
	void getResourcesAsUser() throws Exception {
		// when
		mvc.perform(get("/api/environmentReleases/2_3/resources")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void getEvents() throws Exception {
		// given
		EnvironmentRelease environmentRelease = testEnvRelease(1L, 2L, 3L);
		EnvironmentReleaseId environmentReleaseId = environmentRelease.getId();
		ArgoEvents.Event event = new ArgoEvents.Event();
		event.setMessage("Test event");
		ArgoEvents argoEvents = new ArgoEvents(Collections.singletonList(event));

		when(releaseService.getEvents(eq(environmentReleaseId), eq("abcd"), eq("release1-dev"), eq("release1-app"))).thenReturn(Optional.of(argoEvents));

		// when
		mvc.perform(get("/api/environmentReleases/2_3/events")
				.contentType(MediaType.APPLICATION_JSON)
				.param("resourceUid", "abcd")
				.param("resourceNamespace", "release1-dev")
				.param("resourceName", "release1-app")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].message").value("Test event"));

		// then
		verify(releaseService, times(1)).getEvents(eq(environmentReleaseId), eq("abcd"), eq("release1-dev"), eq("release1-app"));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.user})
	void getEventsAsUser() throws Exception {
		// when
		mvc.perform(get("/api/environmentReleases/2_3/events")
				.contentType(MediaType.APPLICATION_JSON)
				.param("resourceUid", "abcd")
				.param("resourceNamespace", "release1-dev")
				.param("resourceName", "release1-app")
			)
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	private EnvironmentRelease testEnvRelease(Long clusterId, Long environmentId, Long releaseId) {
		Cluster cluster = new Cluster(clusterId, "test-cluster-1", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(environmentId, "dev");
		cluster.addEnvironment(environment);
		Release release = new Release(releaseId, "release1");
		return new EnvironmentRelease(environment, release);
	}

	private String json(Object obj) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		return ow.writeValueAsString(obj);
	}
}
