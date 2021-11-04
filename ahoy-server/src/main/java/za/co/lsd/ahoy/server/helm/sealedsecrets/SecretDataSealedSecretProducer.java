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

package za.co.lsd.ahoy.server.helm.sealedsecrets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.AhoyServerProperties;
import za.co.lsd.ahoy.server.applications.ApplicationSecret;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static za.co.lsd.ahoy.server.util.ProcessUtil.*;

@Component
@Slf4j
public class SecretDataSealedSecretProducer {
	private final AhoyServerProperties serverProperties;

	public SecretDataSealedSecretProducer(AhoyServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	public Map<String, String> produce(ApplicationSecret applicationSecret) throws IOException {
		try {
			log.info("Producing secret data sealed secret for secret: {}", applicationSecret.getName());

			Map<String, String> base64encodedData = applicationSecret.getData().entrySet().stream()
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					entry -> Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8))));

			Secret secret = new SecretBuilder()
				.withNewMetadata().withName(applicationSecret.getName()).endMetadata()
				.withData(base64encodedData)
				.build();
			String secretInput = SerializationUtils.dumpAsYaml(secret);

			ProcessBuilder processBuilder = new ProcessBuilder("kubeseal", "-o", "json", "--scope", "cluster-wide",
				"--controller-name=" + serverProperties.getSealedSecrets().getControllerName(),
				"--controller-namespace=" + serverProperties.getSealedSecrets().getControllerNamespace());
			Process sealedSecretProcess = processBuilder.start();
			inputTo(secretInput, sealedSecretProcess);

			if (sealedSecretProcess.waitFor() == 0) {
				log.info("Successfully produced secret data sealed secret");
				String sealedSecret = outputFrom(sealedSecretProcess);
				return extractEncryptedData(sealedSecret);

			} else {
				String error = errorFrom(sealedSecretProcess);
				throw new IOException("Failed to produce secret data sealed secret: " + error);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to produce secret data sealed secret", e);
		}
	}

	private static Map<String, String> extractEncryptedData(String sealedSecret) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(sealedSecret);
		JsonNode encryptedData = node.at("/spec/encryptedData");

		Iterable<Map.Entry<String, JsonNode>> fields = encryptedData::fields;

		return StreamSupport.stream(fields.spliterator(), false)
			.collect(Collectors.toMap(Map.Entry::getKey, field -> field.getValue().asText()));
	}
}
