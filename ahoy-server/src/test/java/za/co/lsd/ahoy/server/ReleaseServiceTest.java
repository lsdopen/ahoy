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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationReleaseStatusRepository;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseHistory;
import za.co.lsd.ahoy.server.releases.ReleaseHistoryRepository;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.util.Collections;

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
	private ReleaseHistoryRepository releaseHistoryRepository;
	@Autowired
	private ReleaseService releaseService;

	@Test
	public void deploy() throws Exception {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment(1L, "dev", cluster);
		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		DeployDetails deployDetails = new DeployDetails("This is a test commit message");

		when(releaseManager.deploy(same(environmentRelease), same(releaseVersion), same(deployDetails)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease, releaseVersion, deployDetails).get();

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
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion(2L, "1.0.1", release, Collections.singletonList(applicationVersion));

		DeployDetails deployDetails = new DeployDetails("This is a test commit message");

		when(releaseManager.deploy(same(environmentRelease), same(upgradedReleaseVersion), same(deployDetails)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease, upgradedReleaseVersion, deployDetails).get();

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
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		DeployDetails deployDetails = new DeployDetails("This is a test commit message");

		when(releaseManager.deploy(same(environmentRelease), same(releaseVersion), same(deployDetails)))
			.thenReturn(ArgoApplication.builder()
				.metadata(ArgoMetadata.builder()
					.name("test-cluster-dev-release1")
					.uid("test-uid")
					.build()).build());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease, releaseVersion, deployDetails).get();

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
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(1L, 1L), environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0", release, Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		// when
		EnvironmentRelease undeployedEnvironmentRelease = releaseService.undeploy(environmentRelease).get();

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
}
