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
		ApplicationSpec spec = applicationVersion.getSpec();
		ApplicationValues.ApplicationValuesBuilder builder = ApplicationValues.builder()
			.name(applicationVersion.getApplication().getName())
			.version(applicationVersion.getVersion())
			.image(spec.getImage());

		buildDockerRegistry(builder, applicationVersion);
		buildCommandArgs(builder, applicationVersion);
		buildServicePorts(builder, applicationVersion);
		buildHealthChecks(builder, applicationVersion);

		buildEnvironmentVariables(builder, applicationVersion, environmentConfig);
		buildConfigFiles(builder, applicationVersion, environmentConfig);
		buildVolumes(builder, applicationVersion, environmentConfig);
		buildSecrets(builder, applicationVersion, environmentConfig);
		buildResources(builder, applicationVersion, environmentConfig);

		buildReplicas(builder, environmentConfig);
		buildRoute(builder, environmentRelease, applicationVersion, environmentConfig);

		return builder.build();
	}

	private void buildDockerRegistry(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion) throws IOException {
		Optional<DockerRegistry> dockerRegistry = dockerRegistryProvider.dockerRegistryFor(applicationVersion.getSpec().getDockerRegistryName());
		if (dockerRegistry.isPresent() && dockerRegistry.get().getSecure()) {
			builder.dockerConfigJson(dockerConfigSealedSecretProducer.produce(dockerRegistry.get()));
		}
	}

	private void buildCommandArgs(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion) {
		ApplicationSpec spec = applicationVersion.getSpec();
		builder
			.commandArgsEnabled(spec.getCommandArgsEnabled())
			.command(spec.getCommand())
			.args(spec.getArgs());
	}

	private void buildServicePorts(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion) {
		ApplicationSpec spec = applicationVersion.getSpec();
		builder
			.servicePortsEnabled(spec.getServicePortsEnabled())
			.servicePorts(spec.getServicePorts());
	}

	private void buildHealthChecks(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion) {
		ApplicationSpec spec = applicationVersion.getSpec();
		builder
			.healthChecksEnabled(spec.getHealthChecksEnabled())
			.livenessProbe(spec.getLivenessProbe())
			.readinessProbe(spec.getReadinessProbe());
	}

	private void buildEnvironmentVariables(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) {

		Map<String, EnvironmentVariableValues> environmentVariables = new LinkedHashMap<>();

		if (applicationVersion.environmentVariablesEnabled() && applicationVersion.hasEnvironmentVariables()) {
			for (ApplicationEnvironmentVariable environmentVariable : applicationVersion.getSpec().getEnvironmentVariables()) {
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
			.environmentVariablesEnabled(applicationVersion.environmentVariablesEnabled() || (environmentConfig != null && environmentConfig.environmentVariablesEnabled()))
			.environmentVariables(environmentVariables);
	}

	private void buildConfigFiles(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) throws JsonProcessingException {

		Map<String, ApplicationConfigFileValues> configFiles = new LinkedHashMap<>();

		if (applicationVersion.configEnabled() && applicationVersion.hasConfigs()) {
			for (ApplicationConfigFile applicationConfigFile : applicationVersion.getSpec().getConfigFiles()) {
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
			.configFilesEnabled(applicationVersion.configEnabled() || (environmentConfig != null && environmentConfig.configEnabled()))
			.configPath(applicationVersion.getSpec().getConfigPath())
			.configFiles(configFiles)
			.configFileHashes(hashes(configFiles));
	}

	private void buildVolumes(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) {

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();

		if (applicationVersion.volumesEnabled() && applicationVersion.hasVolumes()) {
			for (ApplicationVolume applicationVolume : applicationVersion.getSpec().getVolumes()) {
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
			.volumesEnabled(applicationVersion.volumesEnabled() || (environmentConfig != null && environmentConfig.volumesEnabled()))
			.volumes(volumes);
	}

	private void buildSecrets(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) throws IOException {

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();

		if (applicationVersion.secretsEnabled() && applicationVersion.hasSecrets()) {
			for (ApplicationSecret applicationSecret : applicationVersion.getSpec().getSecrets()) {
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
			.secretsEnabled(applicationVersion.secretsEnabled() || (environmentConfig != null && environmentConfig.secretsEnabled()))
			.secrets(secrets);
	}

	private void buildResources(ApplicationValues.ApplicationValuesBuilder builder, ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) {

		ResourcesValues resourcesValues = new ResourcesValues();

		if (applicationVersion.resourcesEnabled() && applicationVersion.hasResources()) {
			resourcesValues.spec(applicationVersion.getSpec().getResources());
		}

		if (environmentConfig != null) {
			ApplicationEnvironmentSpec environmentSpec = environmentConfig.getSpec();

			if (environmentConfig.resourcesEnabled() && environmentConfig.hasResources()) {
				resourcesValues.spec(environmentSpec.getResources());
			}
		}

		builder
			.resourcesEnabled(applicationVersion.resourcesEnabled() || (environmentConfig != null && environmentConfig.resourcesEnabled()));

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
