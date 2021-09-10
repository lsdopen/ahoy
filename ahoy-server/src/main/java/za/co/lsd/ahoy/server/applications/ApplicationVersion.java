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
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.util.IntegerListConverter;

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
	@OneToOne
	private DockerRegistry dockerRegistry;
	@NotNull
	private String image;
	@NotNull
	private String version;

	@NotNull
	@Convert(converter = IntegerListConverter.class)
	private List<Integer> servicePorts;

	private String healthEndpointPath;
	private Integer healthEndpointPort;
	private String healthEndpointScheme;

	@ManyToOne
	private Application application;

	private String configPath;

	@OneToMany(mappedBy = "applicationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationVersionReference")
	@OrderBy("id")
	private List<ApplicationEnvironmentVariable> environmentVariables;

	@OneToMany(mappedBy = "applicationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationVersionReference")
	@OrderBy("id")
	private List<ApplicationConfig> configs;

	@OneToMany(mappedBy = "applicationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationVersionReference")
	@OrderBy("id")
	private List<ApplicationVolume> volumes;

	@OneToMany(mappedBy = "applicationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("applicationVersionReference")
	@OrderBy("id")
	private List<ApplicationSecret> secrets;

	@ManyToMany(mappedBy = "applicationVersions")
	@JsonIgnore
	@OrderBy("id")
	private List<ReleaseVersion> releaseVersions;

	public ApplicationVersion(@NotNull String version, @NotNull String image, Application application) {
		this.version = version;
		this.image = image;
		this.application = application;
	}

	public boolean hasConfigs() {
		return configs != null && configs.size() > 0;
	}

	public boolean hasVolumes() {
		return volumes != null && volumes.size() > 0;
	}

	public boolean hasSecrets() {
		return secrets != null && secrets.size() > 0;
	}

	@Override
	public String toString() {
		return "ApplicationVersion{" + "id=" + id +
			", image='" + image + '\'' +
			", version='" + version + '\'' +
			", configPath='" + configPath + '\'' +
			'}';
	}
}
