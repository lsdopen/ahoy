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

package za.co.lsd.ahoy.server.helm.sealedsecrets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.AhoyServerProperties;
import za.co.lsd.ahoy.server.docker.DockerRegistry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static za.co.lsd.ahoy.server.util.ProcessUtil.*;

@Component
@Slf4j
public class DockerConfigSealedSecretProducer {
	private final AhoyServerProperties serverProperties;

	public DockerConfigSealedSecretProducer(AhoyServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	public String produce(DockerRegistry dockerRegistry) throws IOException {
		try {
			log.info("Producing docker registry sealed secret for registry: {}", dockerRegistry);
			List<Process> processes = ProcessBuilder.startPipeline(Arrays.asList(
				newDockerRegistrySecretProcessBuilder(dockerRegistry),
				newSealedSecretProcessBuilder()
			));

			Process sealedSecretProcess = processes.get(processes.size() - 1);
			if (sealedSecretProcess.waitFor() == 0) {
				log.info("Successfully produced docker registry sealed secret");
				String sealedSecret = outputFrom(sealedSecretProcess);
				return extractDockerConfigJson(sealedSecret);

			} else {
				String error = errorFrom(sealedSecretProcess);
				throw new IOException("Failed to produce docker config sealed secret: " + error);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to produce docker config sealed secret", e);
		}
	}

	private static ProcessBuilder newDockerRegistrySecretProcessBuilder(DockerRegistry dockerRegistry) {
		switch (dockerRegistry.getCredentials()) {
			case USERNAME_PASSWORD:
				log.debug("Producing docker config secret based on username and password credentials");
				return newUsernamePasswordSecretProcessBuilder(dockerRegistry);
			case DOCKER_CONFIG_JSON:
				log.debug("Producing docker config secret based on docker config json credentials");
				return newDockerConfigJsonSecretProcessBuilder(dockerRegistry);
			default:
				throw new IllegalStateException("Invalid docker registry credentials: " + dockerRegistry.getCredentials());
		}
	}

	private static ProcessBuilder newUsernamePasswordSecretProcessBuilder(DockerRegistry dockerRegistry) {
		return new ProcessBuilder("kubectl", "create", "secret", "docker-registry",
			"docker-registry",
			"--docker-server=" + dockerRegistry.getServer(),
			"--docker-username=" + dockerRegistry.getUsername(),
			"--docker-password=" + dockerRegistry.getPassword(),
			"--dry-run", "-o", "json");
	}

	private static ProcessBuilder newDockerConfigJsonSecretProcessBuilder(DockerRegistry dockerRegistry) {
		return new ProcessBuilder("kubectl", "create", "secret", "generic",
			"docker-cred",
			"--from-literal",
			".dockerconfigjson=" + dockerRegistry.getDockerConfigJson(),
			"--dry-run", "-o", "json");
	}

	private ProcessBuilder newSealedSecretProcessBuilder() {
		return new ProcessBuilder("kubeseal", "-o", "json", "--scope", "cluster-wide",
			"--controller-name=" + serverProperties.getSealedSecrets().getControllerName(),
			"--controller-namespace=" + serverProperties.getSealedSecrets().getControllerNamespace());
	}

	private static String extractDockerConfigJson(String sealedSecret) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(sealedSecret);
		JsonNode dockerConfig = node.at("/spec/encryptedData/.dockerconfigjson");
		return dockerConfig.asText();
	}
}
