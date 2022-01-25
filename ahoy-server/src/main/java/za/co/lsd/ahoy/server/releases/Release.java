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

package za.co.lsd.ahoy.server.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Release implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	@Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$",
		message = "Name invalid: should start with and use lower case letters and numbers")
	private String name;

	@OneToMany(mappedBy = "release")
	@JsonIgnore
	@OrderBy("environment.id")
	@ToString.Exclude
	private List<EnvironmentRelease> environmentReleases = new ArrayList<>();

	@OneToMany(mappedBy = "release", cascade = CascadeType.REMOVE)
	@JsonIgnore
	@OrderBy("id")
	@ToString.Exclude
	private List<ReleaseVersion> releaseVersions = new ArrayList<>();

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

	public void addReleaseVersion(ReleaseVersion releaseVersion) {
		releaseVersions.add(releaseVersion);
		releaseVersion.setRelease(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Release that = (Release) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
