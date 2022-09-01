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

package za.co.lsd.ahoy.server.environments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.AhoyTestServerApplication;
import za.co.lsd.ahoy.server.DeployOptions;
import za.co.lsd.ahoy.server.ReleaseService;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterRepository;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyTestServerApplication.class)
@ActiveProfiles(profiles = {"test", "keycloak"})
class EnvironmentServiceTest {
	@MockBean
	private EnvironmentRepository environmentRepository;
	@MockBean
	private ClusterRepository clusterRepository;
	@MockBean
	private ReleaseService releaseService;
	@MockBean
	private EnvironmentReleaseRepository environmentReleaseRepository;
	@Autowired
	private EnvironmentService environmentService;

	@Test
	void updateOrderIndex() {
		// when
		environmentService.updateOrderIndex(1L, 500.0);

		// then
		verify(environmentRepository, times(1)).updateOrderIndex(eq(1L), eq(500.0));
	}

	@Test
	void duplicate() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);

		Environment sourceEnvironment = new Environment(1L, "dev");
		Environment destEnvironment = new Environment(2L, "qa");
		cluster.addEnvironment(sourceEnvironment);

		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(sourceEnvironment, release);
		environmentRelease.setCurrentReleaseVersion(null);

		when(environmentRepository.findById(1L)).thenReturn(Optional.of(sourceEnvironment));
		when(environmentRepository.findById(2L)).thenReturn(Optional.of(destEnvironment));

		// when
		environmentService.duplicate(sourceEnvironment.getId(), destEnvironment.getId(), new DuplicateOptions(false));

		// then
		verify(environmentRepository, times(1)).findById(sourceEnvironment.getId());
		verify(environmentRepository, times(1)).findById(destEnvironment.getId());
		EnvironmentRelease expectedEnvironmentRelease = new EnvironmentRelease(destEnvironment, release);
		verify(environmentReleaseRepository, times(1)).save(eq(expectedEnvironmentRelease));

		verifyNoMoreInteractions(environmentRepository, environmentReleaseRepository, releaseService);
	}

	@Test
	void duplicateWithEnvironmentConfig() {
		// given
		Cluster cluster = new Cluster(1L, "test-cluster", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);

		Environment sourceEnvironment = new Environment(1L, "dev");
		Environment destEnvironment = new Environment(2L, "qa");
		cluster.addEnvironment(sourceEnvironment);

		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(sourceEnvironment, release);
		environmentRelease.setCurrentReleaseVersion(null);

		when(environmentRepository.findById(1L)).thenReturn(Optional.of(sourceEnvironment));
		when(environmentRepository.findById(2L)).thenReturn(Optional.of(destEnvironment));

		// when
		DuplicateOptions duplicateOptionsWithCopyEnvConfig = new DuplicateOptions(true);
		environmentService.duplicate(sourceEnvironment.getId(), destEnvironment.getId(), duplicateOptionsWithCopyEnvConfig);

		// then
		verify(environmentRepository, times(1)).findById(sourceEnvironment.getId());
		verify(environmentRepository, times(1)).findById(destEnvironment.getId());
		EnvironmentRelease expectedEnvironmentRelease = new EnvironmentRelease(destEnvironment, release);
		verify(environmentReleaseRepository, times(1)).save(eq(expectedEnvironmentRelease));
		verify(releaseService, times(1)).copyEnvironmentConfig(same(environmentRelease), eq(expectedEnvironmentRelease));

		verifyNoMoreInteractions(environmentRepository, environmentReleaseRepository, releaseService);
	}

	@Test
	void moveNoPreviousDeployments() {
		// given
		Cluster cluster1 = new Cluster(1L, "test-cluster-1", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);
		Cluster cluster2 = new Cluster(2L, "test-cluster-2", "https://kubernetes2.default.svc", ClusterType.KUBERNETES);

		Environment environment = new Environment(1L, "dev");
		cluster1.addEnvironment(environment);

		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		environmentRelease.setCurrentReleaseVersion(null);

		when(environmentRepository.findById(1L)).thenReturn(Optional.of(environment));
		when(clusterRepository.findById(2L)).thenReturn(Optional.of(cluster2));
		when(environmentRepository.save(same(environment))).thenReturn(environment);

		// when
		MoveOptions moveOptions = new MoveOptions(2L, true);
		environmentService.move(1L, moveOptions);

		// then
		ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
		verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());
		Environment savedEnvironment = environmentArgumentCaptor.getValue();
		assertEquals(2L, savedEnvironment.getCluster().getId());

		verifyNoMoreInteractions(releaseService);
	}

	@Test
	void moveWithRedeploy() throws ExecutionException, InterruptedException {
		// given
		Cluster cluster1 = new Cluster(1L, "test-cluster-1", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);
		Cluster cluster2 = new Cluster(2L, "test-cluster-2", "https://kubernetes2.default.svc", ClusterType.KUBERNETES);

		Environment environment = new Environment(1L, "dev");
		cluster1.addEnvironment(environment);

		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0");
		release.addReleaseVersion(releaseVersion);
		releaseVersion.setApplicationVersions(Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		// The future is needed for deploy/undeploy but the value is never used
		Future<EnvironmentRelease> environmentReleaseFuture = mock(Future.class);
		when(environmentReleaseFuture.get()).thenReturn(new EnvironmentRelease());

		when(environmentRepository.findById(1L)).thenReturn(Optional.of(environment));
		when(clusterRepository.findById(2L)).thenReturn(Optional.of(cluster2));
		when(releaseService.undeploy(same(environmentRelease.getId()))).thenReturn(environmentReleaseFuture);
		when(environmentRepository.save(same(environment))).thenReturn(environment);
		when(releaseService.deploy(same(environmentRelease.getId()), any(DeployOptions.class))).thenReturn(new EnvironmentRelease());

		// when
		MoveOptions moveOptions = new MoveOptions(2L, true);
		environmentService.move(1L, moveOptions);

		// then
		verify(releaseService, times(1)).undeploy(same(environmentRelease.getId()));

		ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
		verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());
		Environment savedEnvironment = environmentArgumentCaptor.getValue();
		assertEquals(2L, savedEnvironment.getCluster().getId());

		verify(releaseService, times(1)).deploy(same(environmentRelease.getId()), any(DeployOptions.class));
		verifyNoMoreInteractions(releaseService);
	}

	@Test
	void moveWithoutRedeploy() throws ExecutionException, InterruptedException {
		// given
		Cluster cluster1 = new Cluster(1L, "test-cluster-1", "https://kubernetes1.default.svc", ClusterType.KUBERNETES);
		Cluster cluster2 = new Cluster(2L, "test-cluster-2", "https://kubernetes2.default.svc", ClusterType.KUBERNETES);

		Environment environment = new Environment(1L, "dev");
		cluster1.addEnvironment(environment);

		Release release = new Release(1L, "release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", application);
		ReleaseVersion releaseVersion = new ReleaseVersion(1L, "1.0.0");
		release.addReleaseVersion(releaseVersion);
		releaseVersion.setApplicationVersions(Collections.singletonList(applicationVersion));

		environmentRelease.setCurrentReleaseVersion(releaseVersion);

		// The future is needed for deploy/undeploy but the value is never used
		Future<EnvironmentRelease> environmentReleaseFuture = mock(Future.class);
		when(environmentReleaseFuture.get()).thenReturn(new EnvironmentRelease());

		when(environmentRepository.findById(1L)).thenReturn(Optional.of(environment));
		when(clusterRepository.findById(2L)).thenReturn(Optional.of(cluster2));
		when(releaseService.undeploy(same(environmentRelease.getId()))).thenReturn(environmentReleaseFuture);
		when(environmentRepository.save(same(environment))).thenReturn(environment);

		// when
		MoveOptions moveOptions = new MoveOptions(2L, false);
		environmentService.move(1L, moveOptions);

		// then
		verify(releaseService, times(1)).undeploy(same(environmentRelease.getId()));

		ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
		verify(environmentRepository, times(1)).save(environmentArgumentCaptor.capture());
		Environment savedEnvironment = environmentArgumentCaptor.getValue();
		assertEquals(2L, savedEnvironment.getCluster().getId());

		verifyNoMoreInteractions(releaseService);
	}
}
