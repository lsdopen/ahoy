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

package za.co.lsd.ahoy.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentRepository;
import za.co.lsd.ahoy.server.releases.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = "test")
public class ReleaseServiceTest {
	@MockBean
	private ReleaseManager releaseManager;
	@MockBean
	private EnvironmentReleaseRepository environmentReleaseRepository;
	@MockBean
	private ApplicationReleaseStatusRepository applicationReleaseStatusRepository;
	@MockBean
	private ApplicationVersionRepository applicationVersionRepository;
	@MockBean
	private ReleaseHistoryRepository releaseHistoryRepository;
	@MockBean
	private ReleaseVersionRepository releaseVersionRepository;
	@MockBean
	private ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository;
	@MockBean
	private ApplicationEnvironmentConfigProvider environmentConfigProvider;
	@MockBean
	private EnvironmentRepository environmentRepository;
	@Autowired
	private ReleaseService releaseService;

	@Test
	public void deploy() throws Exception {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		long releaseVersionId = 1L;
		ReleaseVersion releaseVersion = new ReleaseVersion(releaseVersionId, "1.0.0", release, Collections.singletonList(applicationVersion));

		DeployOptions deployOptions = new DeployOptions(releaseVersionId, "This is a test commit message");

		when(environmentReleaseRepository.findById(environmentReleaseId)).thenReturn(Optional.of(environmentRelease));
		when(releaseVersionRepository.findById(releaseVersionId)).thenReturn(Optional.of(releaseVersion));
		when(releaseManager.deploy(same(environmentRelease), same(releaseVersion), same(deployOptions)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentReleaseId, deployOptions).get();

		// then
		assertEquals(releaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion(), "Release version should now be the current release version");
		assertEquals("test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName(), "Argo CD name should be set");
		assertEquals("test-uid", deployedEnvironmentRelease.getArgoCdUid(), "Argo CD uid should be set");

		verify(applicationReleaseStatusRepository, never()).deleteAll(any());
		verify(environmentReleaseRepository, times(1)).save(same(deployedEnvironmentRelease));
		verify(releaseHistoryRepository, times(1)).save(any(ReleaseHistory.class));
	}

	@Test
	public void deployUpgrade() throws Exception {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		long upgradedReleaseVersionId = 2L;
		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion(upgradedReleaseVersionId, "1.0.1", release, Collections.singletonList(applicationVersion));

		DeployOptions deployOptions = new DeployOptions(upgradedReleaseVersionId, "This is a test commit message");

		when(environmentReleaseRepository.findById(environmentReleaseId)).thenReturn(Optional.of(environmentRelease));
		when(releaseVersionRepository.findById(upgradedReleaseVersionId)).thenReturn(Optional.of(upgradedReleaseVersion));
		when(releaseManager.deploy(same(environmentRelease), same(upgradedReleaseVersion), same(deployOptions)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentReleaseId, deployOptions).get();

		// then
		assertEquals(upgradedReleaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion(), "Release version should now be the current release version");
		assertEquals("test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName(), "Argo CD name should be set");
		assertEquals("test-uid", deployedEnvironmentRelease.getArgoCdUid(), "Argo CD uid should be set");

		assertEquals(0, (int) deployedEnvironmentRelease.getApplicationsReady(), "Applications ready incorrect");
		assertEquals(HealthStatus.StatusCode.Progressing, deployedEnvironmentRelease.getStatus(), "Status incorrect");
		assertEquals(releaseVersion, deployedEnvironmentRelease.getPreviousReleaseVersion(), "Previous release version incorrect");

		verify(applicationReleaseStatusRepository, times(1)).deleteAll(any());
		verify(environmentReleaseRepository, times(1)).save(same(deployedEnvironmentRelease));
		verify(releaseHistoryRepository, times(1)).save(any(ReleaseHistory.class));
	}

	@Test
	public void deployRedeploy() throws Exception {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		long releaseVersionId = 1L;
		ReleaseVersion releaseVersion = new ReleaseVersion(releaseVersionId, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		DeployOptions deployOptions = new DeployOptions(releaseVersionId, "This is a test commit message");

		when(environmentReleaseRepository.findById(environmentReleaseId)).thenReturn(Optional.of(environmentRelease));
		when(releaseVersionRepository.findById(releaseVersionId)).thenReturn(Optional.of(releaseVersion));
		when(releaseManager.deploy(same(environmentRelease), same(releaseVersion), same(deployOptions)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentReleaseId, deployOptions).get();

		// then
		assertEquals(releaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion(), "Release version should now be the current release version");
		assertEquals("test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName(), "Argo CD name should be set");
		assertEquals("test-uid", deployedEnvironmentRelease.getArgoCdUid(), "Argo CD uid should be set");

		verify(applicationReleaseStatusRepository, never()).deleteAll(any());
		verify(environmentReleaseRepository, times(1)).save(same(deployedEnvironmentRelease));
		verify(releaseHistoryRepository, times(1)).save(any(ReleaseHistory.class));
	}

	@Test
	public void undeploy() throws Exception {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		when(environmentReleaseRepository.findById(environmentReleaseId)).thenReturn(Optional.of(environmentRelease));

		// when
		EnvironmentRelease undeployedEnvironmentRelease = releaseService.undeploy(environmentReleaseId).get();

		// then
		assertNull(undeployedEnvironmentRelease.getCurrentReleaseVersion(), "Release version should be null");
		assertNull(undeployedEnvironmentRelease.getArgoCdName(), "Argo CD name should be null");
		assertNull(undeployedEnvironmentRelease.getArgoCdUid(), "Argo CD uid should be null");

		assertNull(undeployedEnvironmentRelease.getApplicationsReady(), "Applications ready should be null");
		assertNull(undeployedEnvironmentRelease.getStatus(), "Status should be null");
		assertNull(undeployedEnvironmentRelease.getPreviousReleaseVersion(), "Previous release version should be null");

		verify(applicationReleaseStatusRepository, times(1)).deleteAll(any());
		verify(environmentReleaseRepository, times(1)).save(same(undeployedEnvironmentRelease));
		verify(releaseHistoryRepository, times(1)).save(any(ReleaseHistory.class));
	}

	@Test
	public void upgrade() {
		// given
		Release release = new Release(1L, "release1");

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		UpgradeOptions upgradeOptions = new UpgradeOptions("1.1.0", false);

		when(releaseVersionRepository.findById(1L)).thenReturn(Optional.of(releaseVersion));
		ReleaseVersion resultReleaseVersion = new ReleaseVersion(2L, upgradeOptions.getVersion(), releaseVersion.getRelease(), new ArrayList<>(releaseVersion.getApplicationVersions()));
		when(releaseVersionRepository.save(any(ReleaseVersion.class))).thenReturn(resultReleaseVersion);

		// when
		ReleaseVersion upgradedReleaseVersion = releaseService.upgrade(releaseVersion.getId(), upgradeOptions);

		// then
		ArgumentCaptor<ReleaseVersion> releaseVersionCaptor = ArgumentCaptor.forClass(ReleaseVersion.class);
		verify(releaseVersionRepository, times(1)).save(releaseVersionCaptor.capture());

		ReleaseVersion savedReleaseVersion = releaseVersionCaptor.getValue();
		assertEquals("1.1.0", savedReleaseVersion.getVersion(), "Saved release version incorrect");
		assertEquals(release, savedReleaseVersion.getRelease(), "Saved release version has incorrect release");
		assertEquals(releaseVersion.getApplicationVersions(), savedReleaseVersion.getApplicationVersions(), "Saved released version doesn't have the application versions from the upgraded version");

		assertSame(resultReleaseVersion, upgradedReleaseVersion, "The saved release version should have been returned");
		verifyNoInteractions(applicationEnvironmentConfigRepository);
	}

	@Test
	public void upgradeWithCopyEnvironmentConfig() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion(1L, "1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		UpgradeOptions upgradeOptions = new UpgradeOptions("1.1.0", true);

		when(releaseVersionRepository.findById(1L)).thenReturn(Optional.of(releaseVersion));
		ReleaseVersion resultReleaseVersion = new ReleaseVersion(2L, upgradeOptions.getVersion(), releaseVersion.getRelease(), new ArrayList<>(releaseVersion.getApplicationVersions()));
		when(releaseVersionRepository.save(any(ReleaseVersion.class))).thenReturn(resultReleaseVersion);

		when(environmentReleaseRepository.findByRelease_Id_OrderByEnvironmentOrderIndex(release.getId())).thenReturn(Collections.singletonList(environmentRelease));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)).thenReturn(Optional.of(environmentConfig));
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, resultReleaseVersion, applicationVersion)).thenReturn(Optional.empty());

		// when
		ReleaseVersion upgradedReleaseVersion = releaseService.upgrade(releaseVersion.getId(), upgradeOptions);

		// then
		ArgumentCaptor<ReleaseVersion> releaseVersionCaptor = ArgumentCaptor.forClass(ReleaseVersion.class);
		verify(releaseVersionRepository, times(1)).save(releaseVersionCaptor.capture());

		ReleaseVersion savedReleaseVersion = releaseVersionCaptor.getValue();
		assertEquals("1.1.0", savedReleaseVersion.getVersion(), "Saved release version incorrect");
		assertEquals(release, savedReleaseVersion.getRelease(), "Saved release version has incorrect release");
		assertEquals(releaseVersion.getApplicationVersions(), savedReleaseVersion.getApplicationVersions(), "Saved released version doesn't have the application versions from the upgraded version");

		assertSame(resultReleaseVersion, upgradedReleaseVersion, "The saved release version should have been returned");

		ArgumentCaptor<ApplicationEnvironmentConfig> applicationEnvironmentConfigArgumentCaptor = ArgumentCaptor.forClass(ApplicationEnvironmentConfig.class);
		verify(applicationEnvironmentConfigRepository, times(1)).save(applicationEnvironmentConfigArgumentCaptor.capture());

		ApplicationEnvironmentConfig savedEnvironmentConfig = applicationEnvironmentConfigArgumentCaptor.getValue();
		assertEquals(new ApplicationDeploymentId(environmentRelease.getId(), resultReleaseVersion.getId(), applicationVersion.getId()), savedEnvironmentConfig.getId(),
			"Environment config deployment ID incorrect; this means the config is not related to the correct entity");
		assertEquals("myapp1-route", savedEnvironmentConfig.getRouteHostname(), "Environment config route incorrect");
		assertEquals(8080, savedEnvironmentConfig.getRouteTargetPort(), "Environment config port incorrect");
	}

	@Test
	public void promote() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Environment destEnvironment = new Environment(2L, "qa", cluster);

		PromoteOptions promoteOptions = new PromoteOptions(destEnvironment.getId(), false);

		when(environmentReleaseRepository.findById(environmentRelease.getId())).thenReturn(Optional.of(environmentRelease));
		EnvironmentReleaseId resultEnvironmentReleaseId = new EnvironmentReleaseId(destEnvironment.getId(), release.getId());
		when(environmentReleaseRepository.findById(resultEnvironmentReleaseId)).thenReturn(Optional.empty());
		when(environmentRepository.findById(destEnvironment.getId())).thenReturn(Optional.of(destEnvironment));

		EnvironmentRelease resultEnvironmentRelease = new EnvironmentRelease(resultEnvironmentReleaseId, destEnvironment, release);
		when(environmentReleaseRepository.save(any(EnvironmentRelease.class))).thenReturn(resultEnvironmentRelease);

		// when
		EnvironmentRelease promotedEnvironmentRelease = releaseService.promote(environmentReleaseId, promoteOptions);

		// then
		ArgumentCaptor<EnvironmentRelease> environmentReleaseArgumentCaptor = ArgumentCaptor.forClass(EnvironmentRelease.class);
		verify(environmentReleaseRepository, times(1)).save(environmentReleaseArgumentCaptor.capture());

		EnvironmentRelease savedEnvironmentRelease = environmentReleaseArgumentCaptor.getValue();
		assertEquals(release, savedEnvironmentRelease.getRelease(), "Saved environment release has incorrect release");
		assertEquals(destEnvironment, savedEnvironmentRelease.getEnvironment(), "Saved environment release has incorrect environment");

		assertSame(resultEnvironmentRelease, promotedEnvironmentRelease, "The saved environment release should have been returned");
		verifyNoInteractions(applicationEnvironmentConfigRepository);
	}

	@Test
	public void promoteAlreadyExists() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Environment destEnvironment = new Environment(2L, "qa", cluster);

		PromoteOptions promoteOptions = new PromoteOptions(destEnvironment.getId(), false);

		when(environmentReleaseRepository.findById(environmentRelease.getId())).thenReturn(Optional.of(environmentRelease));
		EnvironmentReleaseId resultEnvironmentReleaseId = new EnvironmentReleaseId(destEnvironment.getId(), release.getId());
		EnvironmentRelease resultEnvironmentRelease = new EnvironmentRelease(resultEnvironmentReleaseId, destEnvironment, release);
		when(environmentReleaseRepository.findById(resultEnvironmentReleaseId)).thenReturn(Optional.of(resultEnvironmentRelease));

		// when
		EnvironmentRelease promotedEnvironmentRelease = releaseService.promote(environmentReleaseId, promoteOptions);

		// then
		verify(environmentReleaseRepository, never()).save(any(EnvironmentRelease.class));

		assertSame(resultEnvironmentRelease, promotedEnvironmentRelease, "The saved environment release should have been returned");
		verifyNoInteractions(applicationEnvironmentConfigRepository);
	}

	@Test
	public void promoteWithCopyEnvironmentConfig() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(1L, 1L);
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environmentReleaseId, environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));
		release.setReleaseVersions(Collections.singletonList(releaseVersion));

		Environment destEnvironment = new Environment(2L, "qa", cluster);

		PromoteOptions promoteOptions = new PromoteOptions(destEnvironment.getId(), true);

		when(environmentReleaseRepository.findById(environmentRelease.getId())).thenReturn(Optional.of(environmentRelease));
		EnvironmentReleaseId resultEnvironmentReleaseId = new EnvironmentReleaseId(destEnvironment.getId(), release.getId());
		when(environmentReleaseRepository.findById(resultEnvironmentReleaseId)).thenReturn(Optional.empty());
		when(environmentRepository.findById(destEnvironment.getId())).thenReturn(Optional.of(destEnvironment));

		EnvironmentRelease resultEnvironmentRelease = new EnvironmentRelease(resultEnvironmentReleaseId, destEnvironment, release);
		when(environmentReleaseRepository.save(any(EnvironmentRelease.class))).thenReturn(resultEnvironmentRelease);

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)).thenReturn(Optional.of(environmentConfig));
		when(environmentConfigProvider.environmentConfigFor(resultEnvironmentRelease, releaseVersion, applicationVersion)).thenReturn(Optional.empty());

		// when
		EnvironmentRelease promotedEnvironmentRelease = releaseService.promote(environmentReleaseId, promoteOptions);

		// then
		ArgumentCaptor<EnvironmentRelease> environmentReleaseArgumentCaptor = ArgumentCaptor.forClass(EnvironmentRelease.class);
		verify(environmentReleaseRepository, times(1)).save(environmentReleaseArgumentCaptor.capture());

		EnvironmentRelease savedEnvironmentRelease = environmentReleaseArgumentCaptor.getValue();
		assertEquals(release, savedEnvironmentRelease.getRelease(), "Saved environment release has incorrect release");
		assertEquals(destEnvironment, savedEnvironmentRelease.getEnvironment(), "Saved environment release has incorrect environment");

		assertSame(resultEnvironmentRelease, promotedEnvironmentRelease, "The saved environment release should have been returned");

		ArgumentCaptor<ApplicationEnvironmentConfig> applicationEnvironmentConfigArgumentCaptor = ArgumentCaptor.forClass(ApplicationEnvironmentConfig.class);
		verify(applicationEnvironmentConfigRepository, times(1)).save(applicationEnvironmentConfigArgumentCaptor.capture());

		ApplicationEnvironmentConfig savedEnvironmentConfig = applicationEnvironmentConfigArgumentCaptor.getValue();
		assertEquals(new ApplicationDeploymentId(resultEnvironmentRelease.getId(), releaseVersion.getId(), applicationVersion.getId()), savedEnvironmentConfig.getId(),
			"Environment config deployment ID incorrect; this means the config is not related to the correct entity");
		assertEquals("myapp1-route", savedEnvironmentConfig.getRouteHostname(), "Environment config route incorrect");
		assertEquals(8080, savedEnvironmentConfig.getRouteTargetPort(), "Environment config port incorrect");
	}

	@Test
	public void copyApplicationVersionEnvConfig() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion1 = new ApplicationVersion(1L, "1.0.0", "image", application);
		ApplicationVersion applicationVersion2 = new ApplicationVersion(2L, "2.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion2));

		when(releaseVersionRepository.findById(1L)).thenReturn(Optional.of(releaseVersion));
		when(applicationVersionRepository.findById(1L)).thenReturn(Optional.of(applicationVersion1));
		when(applicationVersionRepository.findById(2L)).thenReturn(Optional.of(applicationVersion2));

		when(environmentReleaseRepository.findByRelease_Id_OrderByEnvironmentOrderIndex(release.getId())).thenReturn(Collections.singletonList(environmentRelease));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion1)).thenReturn(Optional.of(environmentConfig));
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion2)).thenReturn(Optional.empty());

		// when
		releaseService.copyApplicationVersionEnvConfig(releaseVersion.getId(), applicationVersion1.getId(), applicationVersion2.getId());

		// then
		ArgumentCaptor<ApplicationEnvironmentConfig> applicationEnvironmentConfigArgumentCaptor = ArgumentCaptor.forClass(ApplicationEnvironmentConfig.class);
		verify(applicationEnvironmentConfigRepository, times(1)).save(applicationEnvironmentConfigArgumentCaptor.capture());

		ApplicationEnvironmentConfig savedEnvironmentConfig = applicationEnvironmentConfigArgumentCaptor.getValue();
		assertEquals(new ApplicationDeploymentId(environmentRelease.getId(), releaseVersion.getId(), applicationVersion2.getId()), savedEnvironmentConfig.getId(),
			"Environment config deployment ID incorrect; this means the config is not related to the correct entity");
		assertEquals("myapp1-route", savedEnvironmentConfig.getRouteHostname(), "Environment config route incorrect");
		assertEquals(8080, savedEnvironmentConfig.getRouteTargetPort(), "Environment config port incorrect");
	}

	@Test
	public void copyApplicationVersionEnvConfigExistingConfig() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion1 = new ApplicationVersion(1L, "1.0.0", "image", application);
		ApplicationVersion applicationVersion2 = new ApplicationVersion(2L, "2.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion2));

		when(releaseVersionRepository.findById(1L)).thenReturn(Optional.of(releaseVersion));
		when(applicationVersionRepository.findById(1L)).thenReturn(Optional.of(applicationVersion1));
		when(applicationVersionRepository.findById(2L)).thenReturn(Optional.of(applicationVersion2));

		when(environmentReleaseRepository.findByRelease_Id_OrderByEnvironmentOrderIndex(release.getId())).thenReturn(Collections.singletonList(environmentRelease));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion1)).thenReturn(Optional.of(environmentConfig));
		when(environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion2)).thenReturn(Optional.of(environmentConfig));

		// when
		releaseService.copyApplicationVersionEnvConfig(releaseVersion.getId(), applicationVersion1.getId(), applicationVersion2.getId());

		// then
		verify(applicationEnvironmentConfigRepository, never()).save(any(ApplicationEnvironmentConfig.class));
	}
}
