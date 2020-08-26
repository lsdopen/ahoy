/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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
import za.co.lsd.ahoy.server.helm.sealedsecrets.DockerConfigSealedSecretProducer;
import za.co.lsd.ahoy.server.helm.sealedsecrets.SecretDataSealedSecretProducer;
import za.co.lsd.ahoy.server.helm.values.*;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

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
	@MockBean
	private SecretDataSealedSecretProducer secretDataSealedSecretProducer;
	@Autowired
	private Yaml yaml;
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder(Paths.get("./target").toFile());
	private Path repoPath;

	@Before
	public void setupRepoPath() throws IOException {
		repoPath = temporaryFolder.newFolder("repo").toPath();

		when(dockerConfigSealedSecretProducer.produce(any())).thenReturn("encrypted-docker-config");
		when(secretDataSealedSecretProducer.produce(any(ApplicationSecret.class))).thenAnswer(invocationOnMock -> {
			ApplicationSecret applicationSecret = (ApplicationSecret) invocationOnMock.getArguments()[0];
			return applicationSecret.getData();
		});
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
		assertEquals("Incorrect amount of template files", 8, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.replicas(1)
			.environmentVariables(new LinkedHashMap<>())
			.configs(new LinkedHashMap<>())
			.volumes(new LinkedHashMap<>())
			.secrets(new LinkedHashMap<>())
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateFullKubernetes() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		applicationVersion.setDockerRegistry(new DockerRegistry("docker-registry", "docker-server", "username", "password"));
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);
		List<ApplicationEnvironmentVariable> environmentVariables = Arrays.asList(
			new ApplicationEnvironmentVariable("ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_ENV", "my-secret", "secret-key")
		);
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
		environmentConfig.setTls(true);
		environmentConfig.setTlsSecretName("my-tls-secret");
		List<ApplicationConfig> appEnvConfigs = Collections.singletonList(new ApplicationConfig("application-dev.properties", "anothergreeting=hello"));
		environmentConfig.setConfigs(appEnvConfigs);

		List<ApplicationVolume> appVolumes = Arrays.asList(
			new ApplicationVolume("my-volume", "/opt/vol", "standard", VolumeAccessMode.ReadWriteOnce, 2L, StorageUnit.Gi),
			new ApplicationVolume("my-secret-volume", "/opt/secret-vol", "my-secret")
		);
		applicationVersion.setVolumes(appVolumes);

		List<ApplicationSecret> appSecrets = Arrays.asList(
			new ApplicationSecret("my-secret", SecretType.Generic, Collections.singletonMap("secret-key", "secret-value")),
			new ApplicationSecret("my-tls-secret", SecretType.Tls, Collections.singletonMap("cert", "my-cert"))
		);
		applicationVersion.setSecrets(appSecrets);

		List<ApplicationEnvironmentVariable> environmentVariablesEnv = Arrays.asList(
			new ApplicationEnvironmentVariable("DEV_ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_DEV_ENV", "my-secret", "secret-key")
		);
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		List<ApplicationSecret> envSecrets = Collections.singletonList(new ApplicationSecret("my-env-secret", SecretType.Generic, Collections.singletonMap("env-secret-key", "env-secret-value")));
		environmentConfig.setSecrets(envSecrets);

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
		assertEquals("Incorrect amount of template files", 14, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a pvc-app1 template", Files.exists(templatesPath.resolve("pvc-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a ingress-app1 template", Files.exists(templatesPath.resolve("ingress-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-dockerconfig-app1 template", Files.exists(templatesPath.resolve("secret-dockerconfig-app1.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
		assertTrue("We should have a secret-generic-app1 template", Files.exists(templatesPath.resolve("secret-generic-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, EnvironmentVariableValues> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.put("ENV", new EnvironmentVariableValues("ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_ENV", new EnvironmentVariableValues("SECRET_ENV", "my-secret", "secret-key"));
		expectedEnvironmentVariables.put("DEV_ENV", new EnvironmentVariableValues("DEV_ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_DEV_ENV", new EnvironmentVariableValues("SECRET_DEV_ENV", "my-secret", "secret-key"));

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-188deccf", new ApplicationConfigValues("application.properties", "greeting=hello"));
		configs.put("application-config-c1fcd7e5", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();
		volumes.put("application-volume-1", new ApplicationVolumeValues("my-volume", "/opt/vol", "standard", "ReadWriteOnce", "2Gi"));
		volumes.put("application-volume-2", new ApplicationVolumeValues("my-secret-volume", "/opt/secret-vol", "my-secret"));

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		secrets.put("my-secret", new ApplicationSecretValues("my-secret", "Opague", Collections.singletonMap("secret-key", "secret-value")));
		secrets.put("my-tls-secret", new ApplicationSecretValues("my-tls-secret", "kubernetes.io/tls", Collections.singletonMap("cert", "my-cert")));
		secrets.put("my-env-secret", new ApplicationSecretValues("my-env-secret", "Opague", Collections.singletonMap("env-secret-key", "env-secret-value")));

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
			.tls(true)
			.tlsSecretName("my-tls-secret")
			.environmentVariables(expectedEnvironmentVariables)
			.configPath("/opt/config")
			.configs(configs)
			.volumes(volumes)
			.secrets(secrets)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateEnvConfigOnlyKubernetes() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);

		// needed for route
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);

		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		environmentConfig.setReplicas(2);
		environmentConfig.setTls(true);
		environmentConfig.setTlsSecretName("my-tls-secret");
		List<ApplicationConfig> appEnvConfigs = Collections.singletonList(new ApplicationConfig("application-dev.properties", "anothergreeting=hello"));
		environmentConfig.setConfigs(appEnvConfigs);

		List<ApplicationEnvironmentVariable> environmentVariablesEnv = Arrays.asList(
			new ApplicationEnvironmentVariable("DEV_ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_DEV_ENV", "my-secret", "secret-key")
		);
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		List<ApplicationSecret> envSecrets = Arrays.asList(
			new ApplicationSecret("my-env-secret", SecretType.Generic, Collections.singletonMap("env-secret-key", "env-secret-value")),
			new ApplicationSecret("my-tls-secret", SecretType.Tls, Collections.singletonMap("cert", "my-cert"))
		);
		environmentConfig.setSecrets(envSecrets);

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
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a ingress-app1 template", Files.exists(templatesPath.resolve("ingress-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
		assertTrue("We should have a secret-generic-app1 template", Files.exists(templatesPath.resolve("secret-generic-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, EnvironmentVariableValues> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.put("DEV_ENV", new EnvironmentVariableValues("DEV_ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_DEV_ENV", new EnvironmentVariableValues("SECRET_DEV_ENV", "my-secret", "secret-key"));

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-c1fcd7e5", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		secrets.put("my-env-secret", new ApplicationSecretValues("my-env-secret", "Opague", Collections.singletonMap("env-secret-key", "env-secret-value")));
		secrets.put("my-tls-secret", new ApplicationSecretValues("my-tls-secret", "kubernetes.io/tls", Collections.singletonMap("cert", "my-cert")));

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.servicePorts(servicePorts)
			.replicas(2)
			.routeHostname("myapp1-route")
			.routeTargetPort(8080)
			.tls(true)
			.tlsSecretName("my-tls-secret")
			.environmentVariables(expectedEnvironmentVariables)
			.configs(configs)
			.volumes(Collections.emptyMap())
			.secrets(secrets)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
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
		assertEquals("Incorrect amount of template files", 8, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a ingress template", Files.exists(templatesPath.resolve("ingress.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
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
		assertEquals("Incorrect amount of template files", 8, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.replicas(1)
			.environmentVariables(new LinkedHashMap<>())
			.configs(new LinkedHashMap<>())
			.volumes(new LinkedHashMap<>())
			.secrets(new LinkedHashMap<>())
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateFullOpenShift() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://openshift.default.svc", ClusterType.OPENSHIFT);
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		applicationVersion.setDockerRegistry(new DockerRegistry("docker-registry", "docker-server", "username", "password"));
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);
		List<ApplicationEnvironmentVariable> environmentVariables = Arrays.asList(
			new ApplicationEnvironmentVariable("ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_ENV", "my-secret", "secret-key")
		);
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
		List<ApplicationConfig> appEnvConfigs = Collections.singletonList(new ApplicationConfig("application-dev.properties", "anothergreeting=hello"));
		environmentConfig.setConfigs(appEnvConfigs);

		List<ApplicationVolume> appVolumes = Arrays.asList(
			new ApplicationVolume("my-volume", "/opt/vol", "standard", VolumeAccessMode.ReadWriteOnce, 2L, StorageUnit.Gi),
			new ApplicationVolume("my-secret-volume", "/opt/secret-vol", "my-secret")
		);
		applicationVersion.setVolumes(appVolumes);

		List<ApplicationSecret> appSecrets = Collections.singletonList(new ApplicationSecret("my-secret", SecretType.Generic, Collections.singletonMap("secret-key", "secret-value")));
		applicationVersion.setSecrets(appSecrets);

		List<ApplicationEnvironmentVariable> environmentVariablesEnv = Arrays.asList(
			new ApplicationEnvironmentVariable("DEV_ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_DEV_ENV", "my-secret", "secret-key")
		);
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		List<ApplicationSecret> envSecrets = Collections.singletonList(new ApplicationSecret("my-env-secret", SecretType.Generic, Collections.singletonMap("env-secret-key", "env-secret-value")));
		environmentConfig.setSecrets(envSecrets);

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
		assertEquals("Incorrect amount of template files", 14, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a configmap-app1 template", Files.exists(templatesPath.resolve("configmap-app1.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a pvc-app1 template", Files.exists(templatesPath.resolve("pvc-app1.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a route-app1 template", Files.exists(templatesPath.resolve("route-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-dockerconfig-app1 template", Files.exists(templatesPath.resolve("secret-dockerconfig-app1.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
		assertTrue("We should have a secret-generic-app1 template", Files.exists(templatesPath.resolve("secret-generic-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, EnvironmentVariableValues> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.put("ENV", new EnvironmentVariableValues("ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_ENV", new EnvironmentVariableValues("SECRET_ENV", "my-secret", "secret-key"));
		expectedEnvironmentVariables.put("DEV_ENV", new EnvironmentVariableValues("DEV_ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_DEV_ENV", new EnvironmentVariableValues("SECRET_DEV_ENV", "my-secret", "secret-key"));

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-188deccf", new ApplicationConfigValues("application.properties", "greeting=hello"));
		configs.put("application-config-c1fcd7e5", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();
		volumes.put("application-volume-1", new ApplicationVolumeValues("my-volume", "/opt/vol", "standard", "ReadWriteOnce", "2Gi"));
		volumes.put("application-volume-2", new ApplicationVolumeValues("my-secret-volume", "/opt/secret-vol", "my-secret"));

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		secrets.put("my-secret", new ApplicationSecretValues("my-secret", "Opague", Collections.singletonMap("secret-key", "secret-value")));
		secrets.put("my-env-secret", new ApplicationSecretValues("my-env-secret", "Opague", Collections.singletonMap("env-secret-key", "env-secret-value")));

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
			.volumes(volumes)
			.secrets(secrets)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
			.applications(expectedApps)
			.build();

		assertEquals("Values incorrect", expectedValues, actualValues);
	}

	@Test
	public void generateEnvConfigOnlyOpenShift() throws Exception {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://openshift.default.svc", ClusterType.OPENSHIFT);
		cluster.setHost("my-host");
		Environment environment = new Environment("dev", cluster);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);

		Application application = new Application("app1");
		ApplicationVersion applicationVersion = new ApplicationVersion("1.0.0", "image", application);
		List<Integer> servicePorts = Collections.singletonList(8080);
		applicationVersion.setServicePorts(servicePorts);
		ReleaseVersion releaseVersion = new ReleaseVersion("1.0.0", release, Collections.singletonList(applicationVersion));

		ApplicationEnvironmentConfig environmentConfig = new ApplicationEnvironmentConfig("myapp1-route", 8080);
		environmentConfig.setReplicas(2);
		List<ApplicationConfig> appEnvConfigs = Collections.singletonList(new ApplicationConfig("application-dev.properties", "anothergreeting=hello"));
		environmentConfig.setConfigs(appEnvConfigs);

		List<ApplicationEnvironmentVariable> environmentVariablesEnv = Arrays.asList(
			new ApplicationEnvironmentVariable("DEV_ENV", "VAR"),
			new ApplicationEnvironmentVariable("SECRET_DEV_ENV", "my-secret", "secret-key")
		);
		environmentConfig.setEnvironmentVariables(environmentVariablesEnv);

		List<ApplicationSecret> envSecrets = Collections.singletonList(new ApplicationSecret("my-env-secret", SecretType.Generic, Collections.singletonMap("env-secret-key", "env-secret-value")));
		environmentConfig.setSecrets(envSecrets);

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
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a route-app1 template", Files.exists(templatesPath.resolve("route-app1.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a service-app1 template", Files.exists(templatesPath.resolve("service-app1.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
		assertTrue("We should have a secret-generic-app1 template", Files.exists(templatesPath.resolve("secret-generic-app1.yaml")));

		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, EnvironmentVariableValues> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.put("DEV_ENV", new EnvironmentVariableValues("DEV_ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_DEV_ENV", new EnvironmentVariableValues("SECRET_DEV_ENV", "my-secret", "secret-key"));

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-c1fcd7e5", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		secrets.put("my-env-secret", new ApplicationSecretValues("my-env-secret", "Opague", Collections.singletonMap("env-secret-key", "env-secret-value")));

		ApplicationValues expectedApplicationValues = ApplicationValues.builder()
			.name("app1")
			.version("1.0.0")
			.image("image")
			.servicePorts(servicePorts)
			.replicas(2)
			.routeHostname("myapp1-route")
			.routeTargetPort(8080)
			.environmentVariables(expectedEnvironmentVariables)
			.configs(configs)
			.volumes(Collections.emptyMap())
			.secrets(secrets)
			.build();
		Map<String, ApplicationValues> expectedApps = new LinkedHashMap<>();
		expectedApps.put("app1", expectedApplicationValues);

		Values expectedValues = Values.builder()
			.host("my-host")
			.environment("dev")
			.releaseName("release1")
			.releaseVersion("1.0.0")
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
		assertEquals("Incorrect amount of template files", 8, Files.list(templatesPath).filter(Files::isRegularFile).count());
		assertTrue("We should have a configmap template", Files.exists(templatesPath.resolve("configmap.yaml")));
		assertTrue("We should have a pvc template", Files.exists(templatesPath.resolve("pvc.yaml")));
		assertTrue("We should have a deployment template", Files.exists(templatesPath.resolve("deployment.yaml")));
		assertTrue("We should have a deployment-app1 template", Files.exists(templatesPath.resolve("deployment-app1.yaml")));
		assertTrue("We should have a route template", Files.exists(templatesPath.resolve("route.yaml")));
		assertTrue("We should have a service template", Files.exists(templatesPath.resolve("service.yaml")));
		assertTrue("We should have a secret-dockerconfig template", Files.exists(templatesPath.resolve("secret-dockerconfig.yaml")));
		assertTrue("We should have a secret-generic template", Files.exists(templatesPath.resolve("secret-generic.yaml")));
	}
}
