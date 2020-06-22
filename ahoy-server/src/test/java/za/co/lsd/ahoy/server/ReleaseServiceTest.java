package za.co.lsd.ahoy.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
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
		assertEquals("Release version should now be the current release version", releaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals("Argo CD name should be set", "test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName());
		assertEquals("Argo CD uid should be set", "test-uid", deployedEnvironmentRelease.getArgoCdUid());

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
		assertEquals("Release version should now be the current release version", upgradedReleaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals("Argo CD name should be set", "test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName());
		assertEquals("Argo CD uid should be set", "test-uid", deployedEnvironmentRelease.getArgoCdUid());

		assertEquals("Applications ready incorrect", 0, (int) deployedEnvironmentRelease.getApplicationsReady());
		assertEquals("Status incorrect", HealthStatus.StatusCode.Progressing, deployedEnvironmentRelease.getStatus());
		assertEquals("Previous release version incorrect", releaseVersion, deployedEnvironmentRelease.getPreviousReleaseVersion());

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
		assertEquals("Release version should now be the current release version", releaseVersion, deployedEnvironmentRelease.getCurrentReleaseVersion());
		assertEquals("Argo CD name should be set", "test-cluster-dev-release1", deployedEnvironmentRelease.getArgoCdName());
		assertEquals("Argo CD uid should be set", "test-uid", deployedEnvironmentRelease.getArgoCdUid());

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
		assertNull("Release version should be null", undeployedEnvironmentRelease.getCurrentReleaseVersion());
		assertNull("Argo CD name should be null", undeployedEnvironmentRelease.getArgoCdName());
		assertNull("Argo CD uid should be null", undeployedEnvironmentRelease.getArgoCdUid());

		assertNull("Applications ready should be null", undeployedEnvironmentRelease.getApplicationsReady());
		assertNull("Status should be null", undeployedEnvironmentRelease.getStatus());
		assertNull("Previous release version should be null", undeployedEnvironmentRelease.getPreviousReleaseVersion());

		verify(applicationReleaseStatusRepository, times(1)).deleteAll(any());
		verify(environmentReleaseRepository, times(1)).save(same(undeployedEnvironmentRelease));
		verify(releaseHistoryRepository, times(1)).save(any(ReleaseHistory.class));
	}
}
