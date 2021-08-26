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

package za.co.lsd.ahoy.server.helm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
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
	@TempDir
	Path temporaryFolder;
	private Path repoPath;

	@BeforeEach
	public void setupRepoPath() throws IOException {
		repoPath = temporaryFolder.resolve("repo");

		when(dockerConfigSealedSecretProducer.produce(any())).thenReturn("encrypted-docker-config");
		when(secretDataSealedSecretProducer.produce(any(ApplicationSecret.class))).thenAnswer(invocationOnMock -> {
			ApplicationSecret applicationSecret = (ApplicationSecret) invocationOnMock.getArguments()[0];
			return applicationSecret.getData();
		});
	}

	@Test
	public void generateBasic() throws Exception {
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
		assertFilesExist(basePath, "chart",
			"Chart.yaml",
			"values.yaml");

		assertFilesExist(templatesPath, "template",
			"configmap.yaml",
			"pvc.yaml",
			"deployment.yaml",
			"deployment-app1.yaml",
			"ingress.yaml",
			"service.yaml",
			"secret-dockerconfig.yaml",
			"secret-generic.yaml");

		Path valuesPath = basePath.resolve("values.yaml");
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

		assertEquals(expectedValues, actualValues, "Values incorrect");
	}

	@Test
	public void generateFull() throws Exception {
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

		List<ApplicationVolume> envVolumes = Arrays.asList(
			new ApplicationVolume("my-env-volume", "/opt/env-vol", "standard", VolumeAccessMode.ReadWriteOnce, 2L, StorageUnit.Gi),
			new ApplicationVolume("my-env-secret-volume", "/opt/env-secret-vol", "my-env-secret")
		);
		environmentConfig.setVolumes(envVolumes);

		List<ApplicationSecret> envSecrets = Collections.singletonList(new ApplicationSecret("my-env-secret", SecretType.Generic, Collections.singletonMap("env-secret-key", "env-secret-value")));
		environmentConfig.setSecrets(envSecrets);

		when(environmentConfigProvider.environmentConfigFor(any(), any(), any())).thenReturn(Optional.of(environmentConfig));

		Path basePath = repoPath.resolve(cluster.getName()).resolve(environment.getName()).resolve(release.getName());
		Path templatesPath = basePath.resolve("templates");

		// when
		chartGenerator.generate(environmentRelease, releaseVersion, repoPath);

		// then
		assertFilesExist(basePath, "chart",
			"Chart.yaml",
			"values.yaml");

		assertFilesExist(templatesPath, "template",
			"configmap.yaml",
			"configmap-app1.yaml",
			"pvc.yaml",
			"pvc-app1.yaml",
			"deployment.yaml",
			"deployment-app1.yaml",
			"ingress.yaml",
			"ingress-app1.yaml",
			"service.yaml",
			"service-app1.yaml",
			"secret-dockerconfig.yaml",
			"secret-dockerconfig-app1.yaml",
			"secret-generic.yaml",
			"secret-generic-app1.yaml");

		Path valuesPath = basePath.resolve("values.yaml");
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
		volumes.put("my-volume", new ApplicationVolumeValues("my-volume", "/opt/vol", "standard", "ReadWriteOnce", "2Gi"));
		volumes.put("my-secret-volume", new ApplicationVolumeValues("my-secret-volume", "/opt/secret-vol", "my-secret"));
		volumes.put("my-env-volume", new ApplicationVolumeValues("my-env-volume", "/opt/env-vol", "standard", "ReadWriteOnce", "2Gi"));
		volumes.put("my-env-secret-volume", new ApplicationVolumeValues("my-env-secret-volume", "/opt/env-secret-vol", "my-env-secret"));

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

		assertEquals(expectedValues, actualValues, "Values incorrect");
	}

	@Test
	public void generateEnvConfigOnly() throws Exception {
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

		List<ApplicationVolume> envVolumes = Arrays.asList(
			new ApplicationVolume("my-env-volume", "/opt/env-vol", "standard", VolumeAccessMode.ReadWriteOnce, 2L, StorageUnit.Gi),
			new ApplicationVolume("my-env-secret-volume", "/opt/env-secret-vol", "my-env-secret")
		);
		environmentConfig.setVolumes(envVolumes);

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
		assertFilesExist(basePath, "chart",
			"Chart.yaml",
			"values.yaml");

		assertFilesExist(templatesPath, "template",
			"configmap.yaml",
			"configmap-app1.yaml",
			"pvc.yaml",
			"pvc-app1.yaml",
			"deployment.yaml",
			"deployment-app1.yaml",
			"ingress.yaml",
			"ingress-app1.yaml",
			"service.yaml",
			"service-app1.yaml",
			"secret-dockerconfig.yaml",
			"secret-generic.yaml",
			"secret-generic-app1.yaml");

		Path valuesPath = basePath.resolve("values.yaml");
		Values actualValues = yaml.loadAs(Files.newInputStream(valuesPath), Values.class);

		Map<String, EnvironmentVariableValues> expectedEnvironmentVariables = new LinkedHashMap<>();
		expectedEnvironmentVariables.put("DEV_ENV", new EnvironmentVariableValues("DEV_ENV", "VAR"));
		expectedEnvironmentVariables.put("SECRET_DEV_ENV", new EnvironmentVariableValues("SECRET_DEV_ENV", "my-secret", "secret-key"));

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		configs.put("application-config-c1fcd7e5", new ApplicationConfigValues("application-dev.properties", "anothergreeting=hello"));

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();
		volumes.put("my-env-volume", new ApplicationVolumeValues("my-env-volume", "/opt/env-vol", "standard", "ReadWriteOnce", "2Gi"));
		volumes.put("my-env-secret-volume", new ApplicationVolumeValues("my-env-secret-volume", "/opt/env-secret-vol", "my-env-secret"));

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

		assertEquals(expectedValues, actualValues, "Values incorrect");
	}

	@Test
	public void generateBasicPruneTemplateFiles() throws Exception {
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
		assertFalse(Files.exists(extraTemplatePath), "Extra template file should no longer exist");

		assertFilesExist(basePath, "chart",
			"Chart.yaml",
			"values.yaml");

		assertFilesExist(templatesPath, "template",
			"configmap.yaml",
			"pvc.yaml",
			"deployment.yaml",
			"deployment-app1.yaml",
			"ingress.yaml",
			"service.yaml",
			"secret-dockerconfig.yaml",
			"secret-generic.yaml");
	}

	private void assertFilesExist(Path path, String fileType, String... files) throws IOException {
		for (String file : files) {
			assertTrue(Files.exists(path.resolve(file)), "We should have a " + file + " " + fileType);
		}
		// make sure we don't have any more unexpected files
		assertEquals(files.length, Files.list(path).filter(Files::isRegularFile).count(), "Incorrect amount of " + fileType + " files");
	}
}
