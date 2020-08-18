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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.helm.sealedsecrets.DockerConfigSealedSecretProducer;
import za.co.lsd.ahoy.server.helm.sealedsecrets.SecretDataSealedSecretProducer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationValues {
	public String name;
	public String image;
	public String dockerConfigJson;
	public String version;
	public List<Integer> servicePorts;
	public Integer replicas;
	public String routeHostname;
	public Integer routeTargetPort;
	public Map<String, String> environmentVariables;
	public String healthEndpointPath;
	public Integer healthEndpointPort;
	public String healthEndpointScheme;
	public Map<String, ApplicationConfigValues> configs;
	public String configPath;
	public Map<String, ApplicationVolumeValues> volumes;
	public Map<String, ApplicationSecretValues> secrets;

	public static ApplicationValues build(ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig, DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer, SecretDataSealedSecretProducer secretDataSealedSecretProducer) throws IOException {
		ApplicationValuesBuilder builder = builder()
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

		Map<String, String> environmentVariables = new LinkedHashMap<>();
		if (applicationVersion.getEnvironmentVariables() != null)
			environmentVariables.putAll(applicationVersion.getEnvironmentVariables());

		Map<String, ApplicationConfigValues> configs = new LinkedHashMap<>();
		if (applicationVersion.getConfigs() != null) {
			int index = 1;
			for (ApplicationConfig applicationConfig : applicationVersion.getConfigs()) {
				configs.put("application-config-" + index++, new ApplicationConfigValues(applicationConfig));
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
			int index = 1;
			for (ApplicationSecret applicationSecret : applicationVersion.getSecrets()) {
				Map<String, String> encryptedData = secretDataSealedSecretProducer.produce(applicationSecret);
				secrets.put("application-secret-" + index++, new ApplicationSecretValues(applicationSecret.getName(), encryptedData));
			}
		}

		if (environmentConfig != null) {
			if (!StringUtils.isEmpty(environmentConfig.getConfigFileName()) && !StringUtils.isEmpty(environmentConfig.getConfigFileContent())) {
				configs.put("application-config-env", new ApplicationConfigValues(environmentConfig));
			}

			if (environmentConfig.getEnvironmentVariables() != null) {
				environmentVariables.putAll(environmentConfig.getEnvironmentVariables());
			}

			builder
				.replicas(environmentConfig.getReplicas() != null ? environmentConfig.getReplicas() : 1)
				.routeHostname(environmentConfig.getRouteHostname())
				.routeTargetPort(environmentConfig.getRouteTargetPort());

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
}
