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
import za.co.lsd.ahoy.server.applications.ApplicationConfig;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.helm.DockerConfigSealedSecretProducer;

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

	public static ApplicationValues build(ApplicationVersion applicationVersion, ApplicationEnvironmentConfig environmentConfig, DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer) throws IOException {
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
			.configs(configs);

		return builder.build();
	}
}
