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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "application_version", columnNames = {"application_id", "version"}))
public class ApplicationVersion implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String image;
	@NotNull
	private String version;
	@NotNull
	@Convert(converter = ApplicationSpecConverter.class)
	@Lob
	private ApplicationSpec spec;

	@OneToOne
	private DockerRegistry dockerRegistry;

	@ManyToOne
	private Application application;

	@ManyToMany(mappedBy = "applicationVersions")
	@JsonIgnore
	@OrderBy("id")
	private List<ReleaseVersion> releaseVersions;

	public ApplicationVersion(@NotNull String version, @NotNull String image, Application application) {
		this.version = version;
		this.image = image;
		this.application = application;
		this.spec = new ApplicationSpec();
	}

	public ApplicationVersion(@NotNull Long id, @NotNull String version, @NotNull String image, Application application) {
		this.id = id;
		this.version = version;
		this.image = image;
		this.application = application;
		this.spec = new ApplicationSpec();
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

	@Override
	public String toString() {
		return "ApplicationVersion{" + "id=" + id +
			", image='" + image + '\'' +
			", version='" + version + '\'' +
			'}';
	}
}
