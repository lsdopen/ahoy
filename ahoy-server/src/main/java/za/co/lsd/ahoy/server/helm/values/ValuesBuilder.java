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
			apps.put(HelmUtils.valuesName(application), buildApplication(environmentRelease, applicationVersion, applicationEnvironmentConfig));

			log.debug("Added values for application '{}' in environment '{}'", application.getName(), environment.getName());
		}
		valuesBuilder.applications(apps);

		return valuesBuilder.build();
	}

	public ApplicationValues buildApplication(EnvironmentRelease environmentRelease, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) throws IOException {
		ApplicationSpec spec = applicationVersion.getSpec();
		ApplicationValues.ApplicationValuesBuilder builder = ApplicationValues.builder()
			.name(applicationVersion.getApplication().getName())
			.version(applicationVersion.getVersion())
			.image(spec.getImage())
			.commandArgsEnabled(spec.getCommandArgsEnabled())
			.command(spec.getCommand())
			.args(spec.getArgs())
			.servicePorts(spec.getServicePorts())
			.healthChecksEnabled(spec.getHealthChecksEnabled())
			.healthEndpointPath(spec.getHealthEndpointPath())
			.healthEndpointPort(spec.getHealthEndpointPort())
			.healthEndpointScheme(spec.getHealthEndpointScheme())
			.livenessProbe(spec.getLivenessProbe())
			.readinessProbe(spec.getReadinessProbe())
			.configPath(spec.getConfigPath());

		Optional<DockerRegistry> dockerRegistry = dockerRegistryProvider.dockerRegistryFor(spec.getDockerRegistryName());
		if (dockerRegistry.isPresent() && dockerRegistry.get().getSecure()) {
			builder.dockerConfigJson(dockerConfigSealedSecretProducer.produce(dockerRegistry.get()));
		}

		Map<String, EnvironmentVariableValues> environmentVariables = new LinkedHashMap<>();
		if (spec.getEnvironmentVariables() != null) {
			for (ApplicationEnvironmentVariable environmentVariable : spec.getEnvironmentVariables()) {
				environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
			}
		}

		Map<String, ApplicationConfigFileValues> configFiles = new LinkedHashMap<>();
		if (spec.getConfigFiles() != null) {
			for (ApplicationConfigFile applicationConfigFile : spec.getConfigFiles()) {
				configFiles.put(configName(applicationConfigFile), new ApplicationConfigFileValues(applicationConfigFile));
			}
		}

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();
		if (spec.getVolumes() != null) {
			for (ApplicationVolume applicationVolume : spec.getVolumes()) {
				volumes.put(applicationVolume.getName(), new ApplicationVolumeValues(applicationVolume));
			}
		}

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		if (spec.getSecrets() != null) {
			for (ApplicationSecret applicationSecret : spec.getSecrets()) {
				Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
				secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
			}
		}

		ResourcesValues resourcesValues = new ResourcesValues();
		if (spec.getResources() != null) {
			resourcesValues.spec(spec.getResources());
		}

		if (environmentConfig != null) {
			ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();
			if (environmentSpec.getEnvironmentVariables() != null) {
				for (ApplicationEnvironmentVariable environmentVariable : environmentSpec.getEnvironmentVariables()) {
					environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
				}
			}

			if (environmentSpec.getConfigFiles() != null) {
				for (ApplicationConfigFile applicationConfigFile : environmentSpec.getConfigFiles()) {
					configFiles.put(configName(applicationConfigFile), new ApplicationConfigFileValues(applicationConfigFile));
				}
			}

			if (environmentSpec.getVolumes() != null) {
				for (ApplicationVolume applicationVolume : environmentSpec.getVolumes()) {
					volumes.put(applicationVolume.getName(), new ApplicationVolumeValues(applicationVolume));
				}
			}

			if (environmentSpec.getSecrets() != null) {
				for (ApplicationSecret applicationSecret : environmentSpec.getSecrets()) {
					Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
					secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
				}
			}

			if (environmentSpec.getResources() != null) {
				resourcesValues.spec(environmentSpec.getResources());
			}

			builder
				.replicas(environmentSpec.getReplicas() != null ? environmentSpec.getReplicas() : 1)
				.routeHostname(routeHostnameResolver.resolve(environmentRelease, applicationVersion.getApplication(), environmentSpec.getRouteHostname()))
				.routeTargetPort(environmentSpec.getRouteTargetPort())
				.tls(environmentSpec.isTls())
				.tlsSecretName(environmentSpec.getTlsSecretName());

		} else {
			builder.replicas(1);
		}

		if (resourcesValues.hasValues()) {
			builder.resources(resourcesValues);
		}

		builder
			.environmentVariables(environmentVariables)
			.configFiles(configFiles)
			.configFileHashes(hashes(configFiles))
			.volumes(volumes)
			.secrets(secrets);

		return builder.build();
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
