package za.co.lsd.ahoy.server;

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
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.*;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
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

		when(releaseService.deploy(eq(environmentReleaseId), eq(deployOptions))).thenReturn(CompletableFuture.completedFuture(environmentRelease));

		// when
		mvc.perform(post("/api/environmentReleases/2_3/deploy")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(deployOptions)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id.environmentId").value(2L))
			.andExpect(jsonPath("$.id.releaseId").value(3L));

		// then
		verify(releaseService, times(1)).deploy(eq(environmentReleaseId), eq(deployOptions));
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

		when(releaseService.undeploy(eq(environmentReleaseId))).thenReturn(CompletableFuture.completedFuture(environmentRelease));

		// when
		mvc.perform(post("/api/environmentReleases/2_3/undeploy")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id.environmentId").value(2L))
			.andExpect(jsonPath("$.id.releaseId").value(3L));

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
		DuplicateOptions duplicateOptions = new DuplicateOptions("release-1-copy", false, false);
		Release duplicatedRelease = new Release(2L, "release-1-copy");

		when(releaseService.duplicate(eq(1L), eq(duplicateOptions))).thenReturn(duplicatedRelease);

		// when
		mvc.perform(post("/api/releases/1/duplicate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(duplicateOptions)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(2L))
			.andExpect(jsonPath("$.name").value("release-1-copy"));

		// then
		verify(releaseService, times(1)).duplicate(eq(1L), eq(duplicateOptions));
	}

	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.developer})
	void duplicateAsDeveloper() throws Exception {
		// given
		DuplicateOptions duplicateOptions = new DuplicateOptions("release-1-copy", false, false);

		// when
		mvc.perform(post("/api/releases/1/duplicate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(duplicateOptions)))
			.andDo(print())
			.andExpect(status().is(403));

		// then
		verifyNoInteractions(releaseService);
	}

	private EnvironmentRelease testEnvRelease(Long clusterId, Long environmentId, Long releaseId) {
		Cluster cluster = new Cluster(clusterId, "test-cluster-1", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(environmentId, "dev", cluster);
		Release release = new Release(releaseId, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(environment.getId(), release.getId());

		return new EnvironmentRelease(environmentReleaseId, environment, release);
	}

	private String json(Object obj) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		return ow.writeValueAsString(obj);
	}
}
