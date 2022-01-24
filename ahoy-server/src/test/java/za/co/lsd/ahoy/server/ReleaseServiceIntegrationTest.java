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

package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationRepository;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.applications.ApplicationVersionRepository;
import za.co.lsd.ahoy.server.argocd.ArgoClient;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterRepository;
import za.co.lsd.ahoy.server.clustermanager.ClusterManager;
import za.co.lsd.ahoy.server.clustermanager.ClusterManagerFactory;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentRepository;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.LocalRepo;
import za.co.lsd.ahoy.server.releases.*;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;
import za.co.lsd.ahoy.server.settings.SettingsProvider;
import za.co.lsd.ahoy.server.settings.SettingsService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@AutoConfigureTestDatabase
@ActiveProfiles(profiles = {"test", "keycloak"})
@Slf4j
class ReleaseServiceIntegrationTest {
	@Autowired
	private ClusterRepository clusterRepository;
	@Autowired
	private EnvironmentRepository environmentRepository;
	@Autowired
	private ReleaseRepository releaseRepository;
	@Autowired
	private ReleaseVersionRepository releaseVersionRepository;
	@Autowired
	private ReleaseHistoryRepository releaseHistoryRepository;
	@Autowired
	private EnvironmentReleaseRepository environmentReleaseRepository;
	@Autowired
	private ApplicationRepository applicationRepository;
	@Autowired
	private ApplicationVersionRepository applicationVersionRepository;

	@Autowired
	private ReleaseService releaseService;
	@Autowired
	private SettingsProvider settingsProvider;
	@Autowired
	private SettingsService settingsService;
	@Autowired
	private LocalRepo localRepo;

	@MockBean
	private ClusterManagerFactory clusterManagerFactory;
	private ClusterManager clusterManager;
	@MockBean
	private ArgoClient argoClient;

	@TempDir
	Path temporaryFolder;

	private Git testRemoteRepo;

	@BeforeEach
	public void init() throws Exception {
		Path testRemoteRepoPath = temporaryFolder.resolve("repo/remote.git");
		Files.createDirectories(testRemoteRepoPath);
		testRemoteRepo = Git.init().setDirectory(testRemoteRepoPath.toFile()).call();

		GitSettings gitSettings = settingsProvider.getGitSettings();
		gitSettings.setRemoteRepoUri(testRemoteRepoPath.toUri().toString());
		settingsService.saveGitSettings(gitSettings);

		clusterManager = mock(ClusterManager.class);
		when(clusterManagerFactory.newManager(any())).thenReturn(clusterManager);
	}

	@AfterEach
	public void cleanup() throws Exception {
		localRepo.delete();
	}

	/**
	 * Tests the deployment of a new release.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void deploy() throws Exception {
		// given
		Cluster cluster = clusterRepository.findById(1L).orElseThrow();
		Environment environment = environmentRepository.save(new Environment("dev", cluster));
		Release release = releaseRepository.save(new Release("release1"));
		EnvironmentRelease environmentRelease = environmentReleaseRepository.save(new EnvironmentRelease(new EnvironmentReleaseId(), environment, release));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));

		ReleaseVersion releaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion)));

		DeployOptions deployOptions = new DeployOptions(releaseVersion.getId(), "This is a test commit message");

		String argoApplicationName = "minikube-dev-release1";
		String argoUid = UUID.randomUUID().toString();
		when(argoClient.getApplication(eq(argoApplicationName))).thenReturn(Optional.empty());
		when(argoClient.createApplication(any())).thenAnswer(invocationOnMock -> {
			ArgoApplication argoApplication = invocationOnMock.getArgument(0);
			argoApplication.getMetadata().setUid(argoUid);
			return argoApplication;
		});

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease.getId(), deployOptions).get();

		// then
		// verify external collaborators
		verify(clusterManager, times(1)).createNamespace("release1-dev");
		verify(argoClient, times(1)).upsertRepository();
		verify(argoClient, times(1)).createRepositoryCertificates();
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName));
		verify(argoClient, times(1)).createApplication(any(ArgoApplication.class));
		verifyNoMoreInteractions(clusterManager, argoClient);

		// verify environment release
		EnvironmentRelease retrievedEnvironmentRelease = environmentReleaseRepository.findById(deployedEnvironmentRelease.getId()).orElseThrow();
		assertEquals(releaseVersion, retrievedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals(argoApplicationName, retrievedEnvironmentRelease.getArgoCdName());
		assertEquals(argoUid, retrievedEnvironmentRelease.getArgoCdUid());

		// verify git commit
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals(1, commits.size(), "Incorrect amount of commits");
		assertEquals("This is a test commit message", commits.get(0).getFullMessage(), "Remote repo should contain the commit");

		// verify release history
		List<ReleaseHistory> releaseHistories = StreamSupport.stream(releaseHistoryRepository.findAll().spliterator(), false).collect(Collectors.toList());
		assertEquals(1, releaseHistories.size());
		ReleaseHistory releaseHistory = releaseHistories.get(0);
		assertEquals(deployedEnvironmentRelease.getId(), releaseHistory.getEnvironmentRelease().getId());
		assertEquals(ReleaseHistoryAction.DEPLOY, releaseHistory.getAction());
		assertEquals(ReleaseHistoryStatus.SUCCESS, releaseHistory.getStatus());
	}

	/**
	 * Tests the deployment of an existing release that has been upgraded to a new release version.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void deployUpgrade() throws Exception {
		// given
		Cluster cluster = clusterRepository.findById(1L).orElseThrow();
		Environment environment = environmentRepository.save(new Environment("dev", cluster));
		Release release = releaseRepository.save(new Release("release1"));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));
		ApplicationVersion upgradedApplicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.1", application));

		ReleaseVersion releaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion)));
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(), environment, release);
		environmentRelease.setCurrentReleaseVersion(releaseVersion); // this release version is deployed
		environmentRelease = environmentReleaseRepository.save(environmentRelease);

		ReleaseVersion upgradedReleaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.1", release, Collections.singletonList(upgradedApplicationVersion)));

		DeployOptions deployOptions = new DeployOptions(upgradedReleaseVersion.getId(), "This is a test commit message");

		String argoApplicationName = "minikube-dev-release1";
		String argoUid = UUID.randomUUID().toString();
		when(argoClient.getApplication(eq(argoApplicationName))).thenReturn(Optional.of(ArgoApplication.builder()
			.metadata(ArgoMetadata.builder()
				.name(argoApplicationName)
				.uid(argoUid)
				.build()).build()));
		when(argoClient.updateApplication(any())).thenAnswer(invocationOnMock -> {
			ArgoApplication argoApplication = invocationOnMock.getArgument(0);
			argoApplication.getMetadata().setUid(argoUid);
			return argoApplication;
		});

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease.getId(), deployOptions).get();

		// then
		// verify external collaborators
		verify(clusterManager, times(1)).createNamespace(eq("release1-dev"));
		verify(argoClient, times(1)).upsertRepository();
		verify(argoClient, times(1)).createRepositoryCertificates();
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName));
		verify(argoClient, times(1)).updateApplication(any(ArgoApplication.class));
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName), eq(true));
		verifyNoMoreInteractions(clusterManager, argoClient);

		// verify environment release
		EnvironmentRelease retrievedEnvironmentRelease = environmentReleaseRepository.findById(deployedEnvironmentRelease.getId()).orElseThrow();
		assertEquals(upgradedReleaseVersion, retrievedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals(argoApplicationName, retrievedEnvironmentRelease.getArgoCdName());
		assertEquals(argoUid, retrievedEnvironmentRelease.getArgoCdUid());

		// verify git commit
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals(1, commits.size(), "Incorrect amount of commits");
		assertEquals("This is a test commit message", commits.get(0).getFullMessage(), "Remote repo should contain the commit");

		// verify release history
		List<ReleaseHistory> releaseHistories = StreamSupport.stream(releaseHistoryRepository.findAll().spliterator(), false).collect(Collectors.toList());
		assertEquals(1, releaseHistories.size());
		ReleaseHistory releaseHistory = releaseHistories.get(0);
		assertEquals(deployedEnvironmentRelease.getId(), releaseHistory.getEnvironmentRelease().getId());
		assertEquals(ReleaseHistoryAction.DEPLOY, releaseHistory.getAction());
		assertEquals(ReleaseHistoryStatus.SUCCESS, releaseHistory.getStatus());
	}

	/**
	 * Tests the redeployment of an existing release that is deployed.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void deployRedeploy() throws Exception {
		// given
		Cluster cluster = clusterRepository.findById(1L).orElseThrow();
		Environment environment = environmentRepository.save(new Environment("dev", cluster));
		Release release = releaseRepository.save(new Release("release1"));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));

		ReleaseVersion releaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion)));
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(), environment, release);
		environmentRelease.setCurrentReleaseVersion(releaseVersion); // this release version is deployed
		environmentRelease = environmentReleaseRepository.save(environmentRelease);

		DeployOptions deployOptions = new DeployOptions(releaseVersion.getId(), "This is a test commit message");

		String argoApplicationName = "minikube-dev-release1";
		String argoUid = UUID.randomUUID().toString();
		when(argoClient.getApplication(eq(argoApplicationName))).thenReturn(Optional.of(ArgoApplication.builder()
			.metadata(ArgoMetadata.builder()
				.name(argoApplicationName)
				.uid(argoUid)
				.build()).build()));
		when(argoClient.updateApplication(any())).thenAnswer(invocationOnMock -> {
			ArgoApplication argoApplication = invocationOnMock.getArgument(0);
			argoApplication.getMetadata().setUid(argoUid);
			return argoApplication;
		});

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.deploy(environmentRelease.getId(), deployOptions).get();

		// then
		// verify external collaborators
		verify(clusterManager, times(1)).createNamespace(eq("release1-dev"));
		verify(argoClient, times(1)).upsertRepository();
		verify(argoClient, times(1)).createRepositoryCertificates();
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName));
		verify(argoClient, times(1)).updateApplication(any(ArgoApplication.class));
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName), eq(true));
		verifyNoMoreInteractions(clusterManager, argoClient);

		// verify environment release
		EnvironmentRelease retrievedEnvironmentRelease = environmentReleaseRepository.findById(deployedEnvironmentRelease.getId()).orElseThrow();
		assertEquals(releaseVersion, retrievedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals(argoApplicationName, retrievedEnvironmentRelease.getArgoCdName());
		assertEquals(argoUid, retrievedEnvironmentRelease.getArgoCdUid());

		// verify git commit
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals(1, commits.size(), "Incorrect amount of commits");
		assertEquals("This is a test commit message", commits.get(0).getFullMessage(), "Remote repo should contain the commit");

		// verify release history
		List<ReleaseHistory> releaseHistories = StreamSupport.stream(releaseHistoryRepository.findAll().spliterator(), false).collect(Collectors.toList());
		assertEquals(1, releaseHistories.size());
		ReleaseHistory releaseHistory = releaseHistories.get(0);
		assertEquals(deployedEnvironmentRelease.getId(), releaseHistory.getEnvironmentRelease().getId());
		assertEquals(ReleaseHistoryAction.DEPLOY, releaseHistory.getAction());
		assertEquals(ReleaseHistoryStatus.SUCCESS, releaseHistory.getStatus());
	}

	/**
	 * Tests the undeployment of an existing release that is currently deployed.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void undeploy() throws Exception {
		// given
		Cluster cluster = clusterRepository.findById(1L).orElseThrow();
		Environment environment = environmentRepository.save(new Environment("dev", cluster));
		Release release = releaseRepository.save(new Release("release1"));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));

		String argoApplicationName = "minikube-dev-release1";
		ReleaseVersion releaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion)));
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(), environment, release);
		environmentRelease.setCurrentReleaseVersion(releaseVersion); // this release version is deployed
		environmentRelease.setArgoCdName(argoApplicationName);
		environmentRelease = environmentReleaseRepository.save(environmentRelease);

		String argoUid = UUID.randomUUID().toString();
		when(argoClient.getApplication(eq(argoApplicationName))).thenReturn(Optional.of(ArgoApplication.builder()
			.metadata(ArgoMetadata.builder()
				.name(argoApplicationName)
				.uid(argoUid)
				.build()).build()));

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.undeploy(environmentRelease.getId()).get();

		// then
		// verify external collaborators
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName));
		verify(argoClient, times(1)).deleteApplication(argoApplicationName);
		verifyNoMoreInteractions(clusterManager, argoClient);

		// verify environment release
		EnvironmentRelease retrievedEnvironmentRelease = environmentReleaseRepository.findById(deployedEnvironmentRelease.getId()).orElseThrow();
		assertNull(retrievedEnvironmentRelease.getCurrentReleaseVersion());
		assertNull(retrievedEnvironmentRelease.getArgoCdName());
		assertNull(retrievedEnvironmentRelease.getArgoCdUid());

		// verify release history
		List<ReleaseHistory> releaseHistories = StreamSupport.stream(releaseHistoryRepository.findAll().spliterator(), false).collect(Collectors.toList());
		assertEquals(1, releaseHistories.size());
		ReleaseHistory releaseHistory = releaseHistories.get(0);
		assertEquals(deployedEnvironmentRelease.getId(), releaseHistory.getEnvironmentRelease().getId());
		assertEquals(ReleaseHistoryAction.UNDEPLOY, releaseHistory.getAction());
		assertEquals(ReleaseHistoryStatus.SUCCESS, releaseHistory.getStatus());
	}

	/**
	 * Tests the undeployment of an existing release that is currently missing in ArgoCD.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void undeployDoesNotExist() throws Exception {
		// given
		Cluster cluster = clusterRepository.findById(1L).orElseThrow();
		Environment environment = environmentRepository.save(new Environment("dev", cluster));
		Release release = releaseRepository.save(new Release("release1"));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));

		String argoApplicationName = "minikube-dev-release1";
		ReleaseVersion releaseVersion = releaseVersionRepository.save(new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion)));
		EnvironmentRelease environmentRelease = new EnvironmentRelease(new EnvironmentReleaseId(), environment, release);
		environmentRelease.setCurrentReleaseVersion(releaseVersion); // this release version is deployed
		environmentRelease.setArgoCdName(argoApplicationName);
		environmentRelease = environmentReleaseRepository.save(environmentRelease);

		when(argoClient.getApplication(eq(argoApplicationName))).thenReturn(Optional.empty());

		// when
		EnvironmentRelease deployedEnvironmentRelease = releaseService.undeploy(environmentRelease.getId()).get();

		// then
		// verify external collaborators
		verify(argoClient, times(1)).getApplication(eq(argoApplicationName));
		verify(argoClient, never()).deleteApplication(argoApplicationName);
		verifyNoMoreInteractions(clusterManager, argoClient);

		// verify environment release
		EnvironmentRelease retrievedEnvironmentRelease = environmentReleaseRepository.findById(deployedEnvironmentRelease.getId()).orElseThrow();
		assertNull(retrievedEnvironmentRelease.getCurrentReleaseVersion());
		assertNull(retrievedEnvironmentRelease.getArgoCdName());
		assertNull(retrievedEnvironmentRelease.getArgoCdUid());

		// verify release history
		List<ReleaseHistory> releaseHistories = StreamSupport.stream(releaseHistoryRepository.findAll().spliterator(), false).collect(Collectors.toList());
		assertEquals(1, releaseHistories.size());
		ReleaseHistory releaseHistory = releaseHistories.get(0);
		assertEquals(deployedEnvironmentRelease.getId(), releaseHistory.getEnvironmentRelease().getId());
		assertEquals(ReleaseHistoryAction.UNDEPLOY, releaseHistory.getAction());
		assertEquals(ReleaseHistoryStatus.SUCCESS, releaseHistory.getStatus());
	}

	/**
	 * Tests the upgrade of a release version.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin, Role.user})
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void upgrade() throws Exception {
		// given
		Release release = releaseRepository.save(new Release("release1"));

		Application application = applicationRepository.save(new Application("app1"));
		ApplicationVersion applicationVersion = applicationVersionRepository.save(new ApplicationVersion("1.0.0", application));

		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));
		release.addReleaseVersion(releaseVersion);
		releaseVersion = releaseVersionRepository.save(releaseVersion);

		UpgradeOptions upgradeOptions = new UpgradeOptions("1.0.1", false);

		// when
		ReleaseVersion upgradedReleaseVersion = releaseService.upgrade(releaseVersion.getId(), upgradeOptions);

		// then
		// verify external collaborators
		verifyNoInteractions(clusterManager, argoClient);

		// verify release
		Release retrievedRelease = releaseRepository.findById(release.getId()).orElseThrow();
		assertTrue(retrievedRelease.getReleaseVersions().stream().anyMatch((rv -> rv.getId().equals(upgradedReleaseVersion.getId()))), "Release should contain upgraded release version");

		// verify release version
		ReleaseVersion retrievedReleaseVersion = releaseVersionRepository.findById(upgradedReleaseVersion.getId()).orElseThrow();
		assertEquals("1.0.1", retrievedReleaseVersion.getVersion());
		assertArrayEquals(releaseVersion.getApplicationVersions().toArray(), retrievedReleaseVersion.getApplicationVersions().toArray());
	}
}
