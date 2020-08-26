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

package za.co.lsd.ahoy.server.helm.values;

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.helm.HelmUtils;
import za.co.lsd.ahoy.server.helm.sealedsecrets.DockerConfigSealedSecretProducer;
import za.co.lsd.ahoy.server.helm.sealedsecrets.SecretDataSealedSecretProducer;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class ValuesBuilder {
	private final ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private final DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer;
	private final SecretDataSealedSecretProducer secretDataSealedSecretProducer;

	public ValuesBuilder(ApplicationEnvironmentConfigProvider environmentConfigProvider, DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer, SecretDataSealedSecretProducer secretDataSealedSecretProducer) {
		this.environmentConfigProvider = environmentConfigProvider;
		this.dockerConfigSealedSecretProducer = dockerConfigSealedSecretProducer;
		this.secretDataSealedSecretProducer = secretDataSealedSecretProducer;
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
			apps.put(HelmUtils.valuesName(application), buildApplication(applicationVersion, applicationEnvironmentConfig));

			log.debug("Added values for application '{}' in environment '{}'", application.getName(), environment.getName());
		}
		valuesBuilder.applications(apps);

		return valuesBuilder.build();
	}

	public ApplicationValues buildApplication(ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig) throws IOException {
		ApplicationValues.ApplicationValuesBuilder builder = ApplicationValues.builder()
			.name(applicationVersion.getApplication().getName())
			.image(applicationVersion.getImage())
			.version(applicationVersion.getVersion())
			.servicePorts(applicationVersion.getServicePorts())
			.healthEndpointPath(applicationVersion.getHealthEndpointPath())
			.healthEndpointPort(applicationVersion.getHealthEndpointPort())
			.healthEndpointScheme(applicationVersion.getHealthEndpointScheme())
			.configPath(applicationVersion.getConfigPath());

		DockerRegistry dockerRegistry = applicationVersion.getDockerRegistry();
		if (dockerRegistry != null && dockerRegistry.getSecure()) {
			builder.dockerConfigJson(dockerConfigSealedSecretProducer.produce(dockerRegistry));
		}

		Map<String, EnvironmentVariableValues> environmentVariables = new LinkedHashMap<>();
		if (applicationVersion.getEnvironmentVariables() != null) {
			for (ApplicationEnvironmentVariable environmentVariable : applicationVersion.getEnvironmentVariables()) {
				environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
			}
		}

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		if (applicationVersion.getConfigs() != null) {
			for (ApplicationConfig applicationConfig : applicationVersion.getConfigs()) {
				configs.put(configName(applicationConfig), new ApplicationConfigValues(applicationConfig));
			}
		}

		Map<String, ApplicationVolumeValues> volumes = new LinkedHashMap<>();
		if (applicationVersion.getVolumes() != null) {
			int index = 1;
			for (ApplicationVolume applicationVolume : applicationVersion.getVolumes()) {
				volumes.put("application-volume-" + index++, new ApplicationVolumeValues(applicationVolume));
			}
		}

		Map<String, ApplicationSecretValues> secrets = new LinkedHashMap<>();
		if (applicationVersion.getSecrets() != null) {
			for (ApplicationSecret applicationSecret : applicationVersion.getSecrets()) {
				Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
				secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
			}
		}

		if (environmentConfig != null) {
			if (environmentConfig.getConfigs() != null) {
				for (ApplicationConfig applicationConfig : environmentConfig.getConfigs()) {
					configs.put(configName(applicationConfig), new ApplicationConfigValues(applicationConfig));
				}
			}

			if (environmentConfig.getEnvironmentVariables() != null) {
				for (ApplicationEnvironmentVariable environmentVariable : environmentConfig.getEnvironmentVariables()) {
					environmentVariables.put(environmentVariable.getKey(), new EnvironmentVariableValues(environmentVariable));
				}
			}

			if (environmentConfig.getSecrets() != null) {
				for (ApplicationSecret applicationSecret : environmentConfig.getSecrets()) {
					Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
					secrets.put(applicationSecret.getName(), new ApplicationSecretValues(applicationSecret.getName(), secretType(applicationSecret), encryptedData));
				}
			}

			builder
				.replicas(environmentConfig.getReplicas() != null ? environmentConfig.getReplicas() : 1)
				.routeHostname(environmentConfig.getRouteHostname())
				.routeTargetPort(environmentConfig.getRouteTargetPort())
				.tls(environmentConfig.isTls())
				.tlsSecretName(environmentConfig.getTlsSecretName());

		} else {
			builder.replicas(1);
		}

		builder
			.environmentVariables(environmentVariables)
			.configs(configs)
			.volumes(volumes)
			.secrets(secrets);

		return builder.build();
	}

	private String configName(ApplicationConfig config) {
		return "application-config-" + Hashing.crc32()
			.hashString(config.getName(), StandardCharsets.UTF_8)
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
