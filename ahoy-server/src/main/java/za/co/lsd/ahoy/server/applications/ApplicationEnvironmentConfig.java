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

package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ApplicationEnvironmentConfig {
	@EmbeddedId
	private ApplicationDeploymentId id;

	private Integer replicas;

	private String routeHostname;
	private Integer routeTargetPort;

	@OneToMany(mappedBy = "applicationEnvironmentConfig", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationEnvironmentConfigReference")
	private List<ApplicationEnvironmentVariable> environmentVariables;

	@OneToMany(mappedBy = "applicationEnvironmentConfig", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationEnvironmentConfigReference")
	private List<ApplicationConfig> configs;

	@OneToMany(mappedBy = "applicationEnvironmentConfig", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationEnvironmentConfigReference")
	private List<ApplicationSecret> secrets;

	public ApplicationEnvironmentConfig(ApplicationDeploymentId id, ApplicationEnvironmentConfig applicationEnvironmentConfig) {
		this.id = id;
		this.replicas = applicationEnvironmentConfig.getReplicas();
		this.routeHostname = applicationEnvironmentConfig.getRouteHostname();
		this.routeTargetPort = applicationEnvironmentConfig.getRouteTargetPort();
		this.environmentVariables = new ArrayList<>(applicationEnvironmentConfig.getEnvironmentVariables());
		this.configs = new ArrayList<>(applicationEnvironmentConfig.getConfigs());
		this.secrets = new ArrayList<>(applicationEnvironmentConfig.getSecrets());
	}

	public ApplicationEnvironmentConfig(String routeHostname, Integer routeTargetPort) {
		this.routeHostname = routeHostname;
		this.routeTargetPort = routeTargetPort;
	}

	public boolean hasConfigs() {
		return configs != null && configs.size() > 0;
	}

	public boolean hasSecrets() {
		return secrets != null && secrets.size() > 0;
	}
}
