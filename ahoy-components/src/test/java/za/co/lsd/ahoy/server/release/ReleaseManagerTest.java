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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.AhoyConstants;
import za.co.lsd.ahoy.server.AhoyTestServerApplication;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.argocd.ArgoClient;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.LocalRepo;
import za.co.lsd.ahoy.server.helm.ChartGenerator;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.settings.SettingsProvider;
import za.co.lsd.ahoy.server.task.TaskProgressService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyTestServerApplication.class)
@ActiveProfiles(profiles = {"test", "keycloak"})
public class ReleaseManagerTest {
	@MockBean
	private LocalRepo localRepo;
	@MockBean
	private ChartGenerator chartGenerator;
	@MockBean
	private ArgoClient argoClient;
	@MockBean
	private SettingsProvider settingsProvider;
	@MockBean
	private TaskProgressService taskProgressService;
	@Autowired
	private ReleaseManager releaseManager;

	@Test
	public void deployCreateApplication() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc");
		Environment environment = new Environment("dev", "development");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0");
		release.addReleaseVersion(releaseVersion);
		releaseVersion.setApplicationVersions(Collections.singletonList(applicationVersion));

		LocalRepo.WorkingTree workingTree = mock(LocalRepo.WorkingTree.class);
		Path testPath = Paths.get("test");
		when(workingTree.getPath()).thenReturn(testPath);
		when(localRepo.requestWorkingTree()).thenReturn(workingTree);
		when(workingTree.push(anyString())).thenReturn(Optional.of(anyString()));
		when(argoClient.getApplication("test-cluster-dev-release1")).thenReturn(Optional.empty());
		when(settingsProvider.getGitSettings()).thenReturn(new GitSettings("https://github.com/test/test-releases.git"));

		DeployOptions deployOptions = new DeployOptions(releaseVersion.getId(), "This is a test commit message");

		// when
		releaseManager.deploy(environmentRelease, releaseVersion, deployOptions);

		// then
		verify(chartGenerator, times(1)).generate(same(environmentRelease), same(releaseVersion), same(testPath));
		verify(workingTree, times(1)).push(anyString());
		verify(workingTree, times(1)).close();
		verify(localRepo, times(1)).push();

		ArgumentCaptor<ArgoApplication> applicationCaptor = ArgumentCaptor.forClass(ArgoApplication.class);
		verify(argoClient, times(1)).createApplication(applicationCaptor.capture());

		ArgoApplication argoApplication = applicationCaptor.getValue();
		// app
		assertEquals("test-cluster-dev-release1", argoApplication.getMetadata().getName());

		// labels
		Map<String, String> labels = argoApplication.getMetadata().getLabels();
		assertEquals(5, labels.size());
		assertEquals(AhoyConstants.MANAGED_BY_LABEL_VALUE, labels.get(ArgoMetadata.MANAGED_BY_LABEL));
		assertEquals("test-cluster", labels.get(ArgoMetadata.CLUSTER_NAME_LABEL));
		assertEquals("dev", labels.get(ArgoMetadata.ENVIRONMENT_NAME_LABEL));
		assertEquals("release1", labels.get(ArgoMetadata.RELEASE_NAME_LABEL));
		assertEquals("1.0.0", labels.get(ArgoMetadata.RELEASE_VERSION_LABEL));

		// spec
		assertEquals("default", argoApplication.getSpec().getProject());

		// source
		assertEquals("https://github.com/test/test-releases.git", argoApplication.getSpec().getSource().getRepoURL());
		assertEquals("test-cluster/dev/release1", argoApplication.getSpec().getSource().getPath());
		assertEquals("HEAD", argoApplication.getSpec().getSource().getTargetRevision());
		assertEquals(1, argoApplication.getSpec().getSource().getHelm().getValueFiles().size());
		assertEquals("values.yaml", argoApplication.getSpec().getSource().getHelm().getValueFiles().get(0));

		// destination
		assertEquals("release1-dev", argoApplication.getSpec().getDestination().getNamespace());
		assertEquals("https://kubernetes.default.svc", argoApplication.getSpec().getDestination().getServer());

		// automated
		assertNotNull(argoApplication.getSpec().getSyncPolicy().getAutomated());
		assertTrue(argoApplication.getSpec().getSyncPolicy().getAutomated().getPrune());
		assertTrue(argoApplication.getSpec().getSyncPolicy().getAutomated().getSelfHeal());
	}

	@Test
	public void deployUpdateApplication() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc");
		Environment environment = new Environment("dev", "development");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0");
		release.addReleaseVersion(releaseVersion);
		releaseVersion.setApplicationVersions(Collections.singletonList(applicationVersion));

		LocalRepo.WorkingTree workingTree = mock(LocalRepo.WorkingTree.class);
		Path testPath = Paths.get("test");
		when(workingTree.getPath()).thenReturn(testPath);
		when(localRepo.requestWorkingTree()).thenReturn(workingTree);
		when(workingTree.push(anyString())).thenReturn(Optional.of(anyString()));
		when(argoClient.getApplication("test-cluster-dev-release1")).thenReturn(Optional.of(new ArgoApplication()));
		when(settingsProvider.getGitSettings()).thenReturn(new GitSettings("https://github.com/test/test-releases.git"));

		DeployOptions deployOptions = new DeployOptions(releaseVersion.getId(), "This is a test commit message");

		// when
		releaseManager.deploy(environmentRelease, releaseVersion, deployOptions);

		// then
		verify(chartGenerator, times(1)).generate(same(environmentRelease), same(releaseVersion), same(testPath));
		verify(workingTree, times(1)).push(anyString());
		verify(workingTree, times(1)).close();
		verify(localRepo, times(1)).push();

		ArgumentCaptor<ArgoApplication> applicationCaptor = ArgumentCaptor.forClass(ArgoApplication.class);
		verify(argoClient, times(1)).updateApplication(applicationCaptor.capture());

		ArgoApplication argoApplication = applicationCaptor.getValue();
		// app
		assertEquals("test-cluster-dev-release1", argoApplication.getMetadata().getName());

		// labels
		Map<String, String> labels = argoApplication.getMetadata().getLabels();
		assertEquals(5, labels.size());
		assertEquals(AhoyConstants.MANAGED_BY_LABEL_VALUE, labels.get(ArgoMetadata.MANAGED_BY_LABEL));
		assertEquals("test-cluster", labels.get(ArgoMetadata.CLUSTER_NAME_LABEL));
		assertEquals("dev", labels.get(ArgoMetadata.ENVIRONMENT_NAME_LABEL));
		assertEquals("release1", labels.get(ArgoMetadata.RELEASE_NAME_LABEL));
		assertEquals("1.0.0", labels.get(ArgoMetadata.RELEASE_VERSION_LABEL));

		// spec
		assertEquals("default", argoApplication.getSpec().getProject());

		// source
		assertEquals("https://github.com/test/test-releases.git", argoApplication.getSpec().getSource().getRepoURL());
		assertEquals("test-cluster/dev/release1", argoApplication.getSpec().getSource().getPath());
		assertEquals("HEAD", argoApplication.getSpec().getSource().getTargetRevision());
		assertEquals(1, argoApplication.getSpec().getSource().getHelm().getValueFiles().size());
		assertEquals("values.yaml", argoApplication.getSpec().getSource().getHelm().getValueFiles().get(0));

		// destination
		assertEquals("release1-dev", argoApplication.getSpec().getDestination().getNamespace());
		assertEquals("https://kubernetes.default.svc", argoApplication.getSpec().getDestination().getServer());

		// automated
		assertNotNull(argoApplication.getSpec().getSyncPolicy().getAutomated());
		assertTrue(argoApplication.getSpec().getSyncPolicy().getAutomated().getPrune());
		assertTrue(argoApplication.getSpec().getSyncPolicy().getAutomated().getSelfHeal());
	}

	@Test
	public void undeployExists() {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc");
		Environment environment = new Environment("dev", "development");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		environmentRelease.setArgoCdName("test-cluster-dev-release1");
		when(argoClient.getApplication(eq("test-cluster-dev-release1"))).thenReturn(Optional.of(new ArgoApplication()));

		// when
		releaseManager.undeploy(environmentRelease);

		// then
		verify(argoClient, times(1)).getApplication("test-cluster-dev-release1");
		verify(argoClient, times(1)).deleteApplication(eq("test-cluster-dev-release1"));
	}

	@Test
	public void undeployDoesNotExist() {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc");
		Environment environment = new Environment("dev", "development");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		environmentRelease.setArgoCdName("test-cluster-dev-release1");
		when(argoClient.getApplication(eq("test-cluster-dev-release1"))).thenReturn(Optional.empty());

		// when
		releaseManager.undeploy(environmentRelease);

		// then
		verify(argoClient, times(1)).getApplication("test-cluster-dev-release1");
		verify(argoClient, never()).deleteApplication(eq("test-cluster-dev-release1"));
	}
}
