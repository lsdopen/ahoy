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

package za.co.lsd.ahoy.server.helm.values;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.docker.DockerRegistryProvider;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.helm.HelmUtils;
import za.co.lsd.ahoy.server.helm.sealedsecrets.DockerConfigSealedSecretProducer;
import za.co.lsd.ahoy.server.helm.sealedsecrets.SecretDataSealedSecretProducer;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.util.HashUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ValuesBuilder {
	private final DockerRegistryProvider dockerRegistryProvider;
	private final ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private final DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer;
	private final SecretDataSealedSecretProducer secretDataSealedSecretProducer;
	private final ObjectMapper objectMapper;
	private RouteHostnameResolver routeHostnameResolver;

	public ValuesBuilder(DockerRegistryProvider dockerRegistryProvider,
						 ApplicationEnvironmentConfigProvider environmentConfigProvider,
						 DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer,
						 SecretDataSealedSecretProducer secretDataSealedSecretProducer,
						 ObjectMapper objectMapper,
						 RouteHostnameResolver routeHostnameResolver) {
		this.dockerRegistryProvider = dockerRegistryProvider;
		this.environmentConfigProvider = environmentConfigProvider;
		this.dockerConfigSealedSecretProducer = dockerConfigSealedSecretProducer;
		this.secretDataSealedSecretProducer = secretDataSealedSecretProducer;
		this.objectMapper = objectMapper;
		this.routeHostnameResolver = routeHostnameResolver;
	}

	public Values build(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion) throws IOException {
		Environment environment = environmentRelease.getEnvironment();
		Cluster cluster = environment.getCluster();

		Values.ValuesBuilder valuesBuilder = Values.builder()
			.host(cluster.getHost())
			.environment(environment.getName())
			.releaseName(releaseVersion.getRelease().getName())
			.releaseVersion(releaseVersion.getVersion());

		Map<String, ApplicationValues> apps = new LinkedHashMap<>();
		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			ApplicationEnvironmentConfig applicationEnvironmentConfig =
				environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)
					.orElse(null);
			apps.put(HelmUtils.valuesName(application), buildApplicationValues(environmentRelease, applicationVersion, applicationEnvironmentConfig));

			log.debug("Added values for application '{}' in environment '{}'", application.getName(), environment.getName());
		}
		valuesBuilder.applications(apps);

		return valuesBuilder.build();
	}

	private ApplicationValues buildApplicationValues(EnvironmentRelease environmentRelease, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) throws IOException {
		ApplicationSpec applicationSpec = applicationVersion.getSpec();

		ApplicationValues.ApplicationValuesBuilder applicationValuesBuilder = ApplicationValues.builder()
			.name(applicationVersion.getApplication().getName())
			.version(applicationVersion.getVersion());
		buildDockerRegistry(applicationValuesBuilder, applicationSpec);
		buildConfigFiles(applicationValuesBuilder, applicationSpec, environmentConfig);
		buildVolumes(applicationValuesBuilder, applicationSpec, environmentConfig);
		buildSecrets(applicationValuesBuilder, applicationSpec, environmentConfig);
		buildReplicas(applicationValuesBuilder, environmentConfig);
		buildRoute(applicationValuesBuilder, environmentRelease, applicationVersion, environmentConfig);

		Map<String, ContainerValues> containerValues = new LinkedHashMap<>();
		Map<String, ContainerValues> initContainerValues = new LinkedHashMap<>();
		for (ContainerSpec containerSpec : applicationSpec.allContainers()) {

			ContainerValues.ContainerValuesBuilder containerValuesBuilder = ContainerValues.builder()
				.name(containerSpec.getName())
				.image(containerSpec.getImage());

			buildCommandArgs(containerValuesBuilder, containerSpec);
			buildServicePorts(containerValuesBuilder, containerSpec);

			buildEnvironmentVariables(containerValuesBuilder, containerSpec, environmentConfig);
			buildResources(containerValuesBuilder, containerSpec, environmentConfig);

			switch(containerSpec.getType()) {
				case Container:
					buildHealthChecks(containerValuesBuilder, containerSpec);
					containerValues.put(containerSpec.getName(), containerValuesBuilder.build());
					break;
				case Init:
					initContainerValues.put(containerSpec.getName(), containerValuesBuilder.build());
					break;
			}
		}
		applicationValuesBuilder.containers(containerValues);
		applicationValuesBuilder.initContainers(initContainerValues);

		return applicationValuesBuilder.build();
	}

	private void buildDockerRegistry(ApplicationValues.ApplicationValuesBuilder builder, ApplicationSpec applicationSpec) throws IOException {
		Optional<DockerRegistry> dockerRegistry = dockerRegistryProvider.dockerRegistryFor(applicationSpec.getDockerRegistryName());
		if (dockerRegistry.isPresent() && dockerRegistry.get().isSecure()) {
			builder.dockerConfigJson(dockerConfigSealedSecretProducer.produce(dockerRegistry.get()));
		}
	}

	private void buildCommandArgs(ContainerValues.ContainerValuesBuilder builder, ContainerSpec containerSpec) {
		builder
			.commandArgsEnabled(containerSpec.getCommandArgsEnabled())
			.command(containerSpec.getCommand())
			.args(containerSpec.getArgs());
	}

	private void buildServicePorts(ContainerValues.ContainerValuesBuilder builder, ContainerSpec containerSpec) {
		builder
			.servicePortsEnabled(containerSpec.getServicePortsEnabled())
			.servicePorts(containerSpec.getServicePorts());
	}

	private void buildHealthChecks(ContainerValues.ContainerValuesBuilder builder, ContainerSpec containerSpec) {
		builder
			.healthChecksEnabled(containerSpec.getHealthChecksEnabled())
			.livenessProbe(containerSpec.getLivenessProbe())
			.readinessProbe(containerSpec.getReadinessProbe());
	}

	private void buildEnvironmentVariables(ContainerValues.ContainerValuesBuilder builder, ContainerSpec containerSpec, ApplicationEnvironmentConfig environmentConfig) {

		Map<String, EnvironmentVariableValues> environmentVariables = new LinkedHashMap<>();

		if (containerSpec.environmentVariablesEnabled() && containerSpec.hasEnvironmentVariables()) {
			for (ApplicationEnvironmentVariable environmentVariable : containerSpec.getEnvironmentVariables()) {
				environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
			}
		}

		if (environmentConfig != null) {
			if (environmentConfig.environmentVariablesEnabled() && environmentConfig.hasEnvironmentVariables()) {
				for (ApplicationEnvironmentVariable environmentVariable : environmentConfig.getSpec().getEnvironmentVariables()) {
					environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
				}
			}
		}

		builder
			.environmentVariablesEnabled(containerSpec.environmentVariablesEnabled() || (environmentConfig != null && environmentConfig.environmentVariablesEnabled()))
			.environmentVariables(environmentVariables);
	}

	private void buildConfigFiles(ApplicationValues.ApplicationValuesBuilder builder, ApplicationSpec applicationSpec, ApplicationEnvironmentConfig environmentConfig) throws JsonProcessingException {

		Map<String, ApplicationConfigFileValues> configFiles = new LinkedHashMap<>();

		if (applicationSpec.configEnabled() && applicationSpec.hasConfigs()) {
			for (ApplicationConfigFile applicationConfigFile : applicationSpec.getConfigFiles()) {
				configFiles.put(configName(applicationConfigFile), new ApplicationConfigFileValues(applicationConfigFile));
			}
		}

		if (environmentConfig != null) {
			if (environmentConfig.configEnabled() && environmentConfig.hasConfigs()) {
				for (ApplicationConfigFile applicationConfigFile : environmentConfig.getSpec().getConfigFiles()) {
					configFiles.put(configName(applicationConfigFile), new ApplicationConfigFileValues(applicationConfigFile));
				}
			}
		}

		builder
			.configFilesEnabled(applicationSpec.configEnabled() || (environmentConfig != null && environmentConfig.configEnabled()))
			.configPath(applicationSpec.getConfigPath())
			.configFiles(configFiles)
			.configFileHashes(hashes(configFiles));
	}

	private void buildVolumes(ApplicationValues.ApplicationValuesBuilder builder, ApplicationSpec applicationSpec, ApplicationEnvironmentConfig environmentConfig) {

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();

		if (applicationSpec.volumesEnabled() && applicationSpec.hasVolumes()) {
			for (ApplicationVolume applicationVolume : applicationSpec.getVolumes()) {
				volumes.put(applicationVolume.getName(), new ApplicationVolumeValues(applicationVolume));
			}
		}

		if (environmentConfig != null) {
			ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();

			if (environmentConfig.volumesEnabled() && environmentConfig.hasVolumes()) {
				for (ApplicationVolume applicationVolume : environmentSpec.getVolumes()) {
					volumes.put(applicationVolume.getName(), new ApplicationVolumeValues(applicationVolume));
				}
			}
		}

		builder
			.volumesEnabled(applicationSpec.volumesEnabled() || (environmentConfig != null && environmentConfig.volumesEnabled()))
			.volumes(volumes);
	}

	private void buildSecrets(ApplicationValues.ApplicationValuesBuilder builder, ApplicationSpec applicationSpec, ApplicationEnvironmentConfig environmentConfig) throws IOException {

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();

		if (applicationSpec.secretsEnabled() && applicationSpec.hasSecrets()) {
			for (ApplicationSecret applicationSecret : applicationSpec.getSecrets()) {
				Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
				secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
			}
		}

		if (environmentConfig != null) {
			ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();

			if (environmentConfig.secretsEnabled() && environmentConfig.hasSecrets()) {
				for (ApplicationSecret applicationSecret : environmentSpec.getSecrets()) {
					Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
					secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
				}
			}
		}

		builder
			.secretsEnabled(applicationSpec.secretsEnabled() || (environmentConfig != null && environmentConfig.secretsEnabled()))
			.secrets(secrets);
	}

	private void buildResources(ContainerValues.ContainerValuesBuilder builder, ContainerSpec containerSpec, ApplicationEnvironmentConfig environmentConfig) {

		ResourcesValues resourcesValues = new ResourcesValues();

		if (containerSpec.resourcesEnabled() && containerSpec.hasResources()) {
			resourcesValues.spec(containerSpec.getResources());
		}

		if (environmentConfig != null) {
			ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();

			if (environmentConfig.resourcesEnabled() && environmentConfig.hasResources()) {
				resourcesValues.spec(environmentSpec.getResources());
			}
		}

		builder
			.resourcesEnabled(containerSpec.resourcesEnabled() || (environmentConfig != null && environmentConfig.resourcesEnabled()));

		if (resourcesValues.hasValues()) {
			builder.resources(resourcesValues);
		}
	}

	private void buildReplicas(ApplicationValues.ApplicationValuesBuilder builder, ApplicationEnvironmentConfig environmentConfig) {

		builder.replicas(1);

		if (environmentConfig != null && environmentConfig.hasReplicas()) {
			builder.replicas(environmentConfig.getSpec().getReplicas());
		}
	}

	private void buildRoute(ApplicationValues.ApplicationValuesBuilder builder, EnvironmentRelease environmentRelease, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) {
		if (environmentConfig != null) {
			if (environmentConfig.routeEnabled() && environmentConfig.hasRoute()) {

				ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();
				builder
					.routeHostname(routeHostnameResolver.resolve(environmentRelease, applicationVersion.getApplication(), environmentSpec.getRouteHostname()))
					.routeTargetPort(environmentSpec.getRouteTargetPort())
					.tls(environmentSpec.isTls())
					.tlsSecretName(environmentSpec.getTlsSecretName());
			}
		}

		builder.routeEnabled(environmentConfig != null && environmentConfig.routeEnabled());
	}

	private String hashes(Map<String, ApplicationConfigFileValues> configFiles) throws JsonProcessingException {
		if (configFiles != null && !configFiles.isEmpty()) {
			Map<String, String> hashes = new LinkedHashMap<>();
			for (Map.Entry<String, ApplicationConfigFileValues> entry : configFiles.entrySet()) {
				ApplicationConfigFileValues configFile = entry.getValue();
				hashes.put(configFile.name, HashUtil.hash(configFile.content));
			}
			return objectMapper.writeValueAsString(hashes);
		} else {
			return null;
		}
	}

	private String configName(ApplicationConfigFile configFile) {
		return "application-config-file-" + Hashing.crc32()
			.hashString(configFile.getName(), StandardCharsets.UTF_8)
			.toString();
	}

	private String secretType(ApplicationSecret applicationSecret) {
		switch (applicationSecret.getType()) {
			case Generic:
				return "Opague";
			case Tls:
				return "kubernetes.io/tls";
			default:
				throw new IllegalStateException("Unhandled secret type");
		}
	}
}
