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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "application_version", columnNames = {"application_id", "version"}))
public class ApplicationVersion implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String version;
	@NotNull
	@Convert(converter = ApplicationSpecConverter.class)
	@Column(length = 10485760)
	@JsonProperty(access = WRITE_ONLY)
	private ApplicationSpec spec;

	@NotNull
	@ManyToOne
	@JsonProperty(access = WRITE_ONLY)
	@ToString.Exclude
	private Application application;

	@ManyToMany(mappedBy = "applicationVersions")
	@OrderBy("id")
	@JsonIgnore
	@ToString.Exclude
	private List<ReleaseVersion> releaseVersions = new ArrayList<>();

	public ApplicationVersion(@NotNull String version, Application application) {
		this.version = version;
		this.application = application;
		this.spec = new ApplicationSpec();
	}

	public ApplicationVersion(@NotNull Long id, @NotNull String version, Application application) {
		this.id = id;
		this.version = version;
		this.application = application;
		this.spec = new ApplicationSpec();
	}

	public boolean configEnabled() {
		return spec != null && spec.getConfigFilesEnabled() != null && spec.getConfigFilesEnabled();
	}

	public boolean hasConfigs() {
		return spec != null && spec.getConfigFiles() != null && spec.getConfigFiles().size() > 0;
	}

	public boolean volumesEnabled() {
		return spec != null && spec.getVolumesEnabled() != null && spec.getVolumesEnabled();
	}

	public boolean hasVolumes() {
		return spec != null && spec.getVolumes() != null && spec.getVolumes().size() > 0;
	}

	public boolean hasSecrets() {
		return spec != null && spec.getSecrets() != null && spec.getSecrets().size() > 0;
	}

	public ApplicationSpec summarySpec() {
		return new ApplicationSpec(spec.getImage(), spec.getDockerRegistryName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		ApplicationVersion that = (ApplicationVersion) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
