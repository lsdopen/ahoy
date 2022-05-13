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

package za.co.lsd.ahoy.server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties("ahoy")
@Data
@Slf4j
public class AhoyServerProperties {
	private String releaseName = "ahoy";
	private String releaseNamespace = "ahoy";
	private String host = "default.host";
	private String clusterType = "kubernetes";
	private String repoPath;
	private Auth auth;
	private SealedSecrets sealedSecrets = new SealedSecrets();

	@PostConstruct
	public void logSummary() {
		log.info(toString());
	}

	@Data
	public static class Auth {
		private String clientId;
		private String issuer;
		private String jwkSetUri;
	}

	@Data
	public static class SealedSecrets {
		private String controllerName = "ahoy-sealed-secrets";
		private String controllerNamespace = "ahoy";
	}
}
