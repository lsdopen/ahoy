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

package za.co.lsd.ahoy.server.applications;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class ApplicationEnvironmentConfig {
	@EmbeddedId
	private ApplicationDeploymentId id;
	@NotNull
	@Convert(converter = ApplicationEnvironmentSpecConverter.class)
	@Column(length = 10485760)
	private ApplicationEnvironmentSpec spec;

	public ApplicationEnvironmentConfig(ApplicationDeploymentId id, ApplicationEnvironmentConfig applicationEnvironmentConfig) {
		this.id = id;
		this.spec = applicationEnvironmentConfig.spec;
	}

	public ApplicationEnvironmentConfig(ApplicationEnvironmentSpec spec) {
		this.spec = spec;
	}

	public boolean hasConfigs() {
		return spec != null && spec.getConfigFiles() != null && spec.getConfigFiles().size() > 0;
	}

	public boolean hasVolumes() {
		return spec != null && spec.getVolumes() != null && spec.getVolumes().size() > 0;
	}

	public boolean hasSecrets() {
		return spec != null && spec.getSecrets() != null && spec.getSecrets().size() > 0;
	}

	public ApplicationEnvironmentSpec leanSpec() {
		return new ApplicationEnvironmentSpec(spec.getRouteHostname());
	}
}
