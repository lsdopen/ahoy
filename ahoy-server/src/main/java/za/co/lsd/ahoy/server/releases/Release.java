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

package za.co.lsd.ahoy.server.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Release implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	private String name;

	@OneToMany(mappedBy = "release")
	@JsonIgnore
	@OrderBy("environment.id")
	private List<EnvironmentRelease> environmentReleases;

	@OneToMany(mappedBy = "release", cascade = CascadeType.REMOVE)
	@JsonIgnore
	@OrderBy("id")
	private List<ReleaseVersion> releaseVersions;

	public ReleaseVersion latestReleaseVersion() {
		if (releaseVersions != null && releaseVersions.size() > 0) {
			return releaseVersions.get(releaseVersions.size() - 1);
		}
		return null;
	}

	public Release(@NotNull String name) {
		this.name = name;
	}

	public Release(Long id, @NotNull String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return "Release{" +
			"id=" + id +
			", name='" + name + '\'' +
			'}';
	}
}
