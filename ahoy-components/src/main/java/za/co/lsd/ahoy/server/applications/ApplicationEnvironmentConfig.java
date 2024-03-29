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

package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ApplicationEnvironmentConfig {
	@EmbeddedId
	private ApplicationDeploymentId id;
	@NotNull
	@Convert(converter = ApplicationEnvironmentSpecConverter.class)
	@Column(length = 10485760)
	@JsonProperty(access = WRITE_ONLY)
	@ToString.Exclude
	private ApplicationEnvironmentSpec spec;

	public ApplicationEnvironmentConfig(ApplicationDeploymentId id, ApplicationEnvironmentConfig applicationEnvironmentConfig) {
		this.id = id;
		this.spec = applicationEnvironmentConfig.spec;
	}

	public ApplicationEnvironmentConfig(ApplicationEnvironmentSpec spec) {
		this.spec = spec;
	}

	public boolean hasReplicas() {
		return spec != null && spec.getReplicas() != null;
	}

	public boolean routeEnabled() {
		return spec != null && spec.isRouteEnabled();
	}

	public boolean hasRoutes() {
		return spec != null && spec.getRoutes() != null && !spec.getRoutes().isEmpty();
	}

	public boolean environmentVariablesEnabled() {
		return spec != null && spec.isEnvironmentVariablesEnabled();
	}

	public boolean hasEnvironmentVariables() {
		return spec != null && spec.getEnvironmentVariables() != null && spec.getEnvironmentVariables().size() > 0;
	}

	public boolean configEnabled() {
		return spec != null && spec.isConfigFilesEnabled();
	}

	public boolean hasConfigs() {
		return spec != null && spec.getConfigFiles() != null && spec.getConfigFiles().size() > 0;
	}

	public boolean volumesEnabled() {
		return spec != null && spec.isVolumesEnabled();
	}

	public boolean hasVolumes() {
		return spec != null && spec.getVolumes() != null && spec.getVolumes().size() > 0;
	}

	public boolean secretsEnabled() {
		return spec != null && spec.isSecretsEnabled();
	}

	public boolean hasSecrets() {
		return spec != null && spec.getSecrets() != null && spec.getSecrets().size() > 0;
	}

	public boolean resourcesEnabled() {
		return spec != null && spec.isResourcesEnabled();
	}

	public boolean hasResources() {
		return spec != null && spec.getResources() != null;
	}

	public ApplicationEnvironmentSpec summarySpec() {
		return ApplicationEnvironmentSpec.newSummarySpec(spec.isRouteEnabled(), spec.getRoutes());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		ApplicationEnvironmentConfig that = (ApplicationEnvironmentConfig) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
