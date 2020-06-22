package za.co.lsd.ahoy.server.helm;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.Yaml;
import za.co.lsd.ahoy.server.AhoyServerApplication;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.helm.values.ApplicationConfigValues;
import za.co.lsd.ahoy.server.helm.values.ApplicationValues;
import za.co.lsd.ahoy.server.helm.values.Values;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = "test")
public class ChartGeneratorTest {
	@Autowired
	private ChartGenerator chartGenerator;
	@MockBean
	private ApplicationEnvironmentConfigProvider environmentConfigProvider;
	@MockBean
	private DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer;
	@Autowired
	private Yaml yaml;
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder(Paths.get("./target").toFile());
	private Path repoPath;

	@Before
	public void setupRepoPath() throws IOException {
		repoPath = temporaryFolder.newFolder("repo").toPath();

		when(dockerConfigSealedSecretProducer.produce(any())).thenReturn("encrypted-docker-config");
	}

	@Test
	public void generateBasicKubernetes() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.empty());

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		Path valuesPath = basePath.resolve("values.yaml");
		assertTrue("We should have a values yaml", Files.exists(valuesPath));
		assertEquals("Incorrect amount of template files", 7, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.replicas(1)
			.environmentVariables(new LinkedHashMap<>())
			.configs(new LinkedHashMap<>())
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.imageNamespace("dev")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateFullKubernetes() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setDockerRegistry("docker-registry");
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		applicationVersion.setDockerRegistry(new DockerRegistry("docker-registry", "docker-server", "username", "password"));
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);
		Map<String, String> environmentVariables = Collections.singletonMap("ENV", "VAR");
		applicationVersion.setEnvironmentVariables(environmentVariables);
		applicationVersion.setHealthEndpointPath("/");
		applicationVersion.setHealthEndpointPort(8080);
		applicationVersion.setHealthEndpointScheme("HTTP");
		applicationVersion.setConfigPath("/opt/config");
		List<ApplicationConfig> appConfigs = Collections.singletonList(new ApplicationConfig("application.properties", "greeting=hello"));
		applicationVersion.setConfigs(appConfigs);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		environmentConfig.setReplicas(2);
		environmentConfig.setConfigFileName("application-dev.properties");
		environmentConfig.setConfigFileContent("anothergreeting=hello");

		Map<String, String> environmentVariablesEnv = Collections.singletonMap("DEV_ENV", "VAR");
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.of(environmentConfig));

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		Path valuesPath = basePath.resolve("values.yaml");
		assertTrue("We should have a values yaml", Files.exists(valuesPath));
		assertEquals("Incorrect amount of template files", 10, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a ingress-app1 template", Files.exists(templatesPath.resolve("ingress-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-dockerconfig-app1 template", Files.exists(templatesPath.resolve("secret-dockerconfig-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, String> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.putAll(environmentVariables);
		expectedEnvironmentVariables.putAll(environmentVariablesEnv);

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-1", new ApplicationConfigValues("application.properties", "greeting=hello"));
		configs.put("application-config-env", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.dockerConfigJson("encrypted-docker-config")
			.servicePorts(servicePorts)
			.healthEndpointPath("/")
			.healthEndpointPort(8080)
			.healthEndpointScheme("HTTP")
			.replicas(2)
			.routeHostname("myapp1-route")
			.routeTargetPort(8080)
			.environmentVariables(expectedEnvironmentVariables)
			.configPath("/opt/config")
			.configs(configs)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.dockerRegistry("docker-registry")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.imageNamespace("dev")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateBasicKubernetesPruneTemplateFiles() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.empty());

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");
		Files.createDirectories(templatesPath);
		Path extraTemplatePath = templatesPath.resolve("extratemplate.yaml");
		Files.writeString(extraTemplatePath, "test");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertFalse("Extra template file should no longer exist", Files.exists(extraTemplatePath));

		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		assertTrue("We should have a values yaml", Files.exists(basePath.resolve("values.yaml")));
		assertEquals("Incorrect amount of template files", 7, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
	}

	@Test
	public void generateBasicOpenShift() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://openshift.default.svc", ClusterType.OPENSHIFT);
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.empty());

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		Path valuesPath = basePath.resolve("values.yaml");
		assertTrue("We should have a values yaml", Files.exists(valuesPath));
		assertEquals("Incorrect amount of template files", 9, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a imagestream template", Files.exists(templatesPath.resolve("imagestream.yaml")));
		assertTrue("We should have a imagestream-app1 template", Files.exists(templatesPath.resolve("imagestream-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.replicas(1)
			.environmentVariables(new LinkedHashMap<>())
			.configs(new LinkedHashMap<>())
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.imageNamespace("dev")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateFullOpenShift() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://openshift.default.svc", ClusterType.OPENSHIFT);
		cluster.setDockerRegistry("docker-registry");
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		applicationVersion.setDockerRegistry(new DockerRegistry("docker-registry", "docker-server", "username", "password"));
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);
		Map<String, String> environmentVariables = Collections.singletonMap("ENV", "VAR");
		applicationVersion.setEnvironmentVariables(environmentVariables);
		applicationVersion.setHealthEndpointPath("/");
		applicationVersion.setHealthEndpointPort(8080);
		applicationVersion.setHealthEndpointScheme("HTTP");
		applicationVersion.setConfigPath("/opt/config");
		List<ApplicationConfig> appConfigs = Collections.singletonList(new ApplicationConfig("application.properties", "greeting=hello"));
		applicationVersion.setConfigs(appConfigs);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		environmentConfig.setReplicas(2);
		environmentConfig.setConfigFileName("application-dev.properties");
		environmentConfig.setConfigFileContent("anothergreeting=hello");

		Map<String, String> environmentVariablesEnv = Collections.singletonMap("DEV_ENV", "VAR");
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.of(environmentConfig));

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		Path valuesPath = basePath.resolve("values.yaml");
		assertTrue("We should have a values yaml", Files.exists(valuesPath));
		assertEquals("Incorrect amount of template files", 12, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a imagestream template", Files.exists(templatesPath.resolve("imagestream.yaml")));
		assertTrue("We should have a imagestream-app1 template", Files.exists(templatesPath.resolve("imagestream-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a route-app1 template", Files.exists(templatesPath.resolve("route-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-dockerconfig-app1 template", Files.exists(templatesPath.resolve("secret-dockerconfig-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, String> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.putAll(environmentVariables);
		expectedEnvironmentVariables.putAll(environmentVariablesEnv);

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-1", new ApplicationConfigValues("application.properties", "greeting=hello"));
		configs.put("application-config-env", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.dockerConfigJson("encrypted-docker-config")
			.servicePorts(servicePorts)
			.healthEndpointPath("/")
			.healthEndpointPort(8080)
			.healthEndpointScheme("HTTP")
			.replicas(2)
			.routeHostname("myapp1-route")
			.routeTargetPort(8080)
			.environmentVariables(expectedEnvironmentVariables)
			.configPath("/opt/config")
			.configs(configs)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.dockerRegistry("docker-registry")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.imageNamespace("dev")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateBasicOpenShiftPruneTemplateFiles() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://openshift.default.svc", ClusterType.OPENSHIFT);
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.empty());

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");
		Files.createDirectories(templatesPath);
		Path extraTemplatePath = templatesPath.resolve("extratemplate.yaml");
		Files.writeString(extraTemplatePath, "test");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertFalse("Extra template file should no longer exist", Files.exists(extraTemplatePath));

		assertEquals("Incorrect amount of chart files", 2, Files.list(basePath).filter(Files::isRegularFile).count());
		assertTrue("We should have a chart yaml", Files.exists(basePath.resolve("Chart.yaml")));
		assertTrue("We should have a values yaml", Files.exists(basePath.resolve("values.yaml")));
		assertEquals("Incorrect amount of template files", 9, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a imagestream template", Files.exists(templatesPath.resolve("imagestream.yaml")));
		assertTrue("We should have a imagestream-app1 template", Files.exists(templatesPath.resolve("imagestream-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
	}
}
