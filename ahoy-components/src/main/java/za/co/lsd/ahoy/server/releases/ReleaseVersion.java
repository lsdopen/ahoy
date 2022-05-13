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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;

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
@Table(uniqueConstraints = @UniqueConstraint(name = "release_version", columnNames = {"release_id", "version"}))
public class ReleaseVersion implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String version;

	@NotNull
	@ManyToOne
	@JsonProperty(access = WRITE_ONLY)
	@ToString.Exclude
	private Release release;

	@ManyToMany
	@JoinTable(name = "RELEASE_VERSION_APPLICATION_VERSIONS",
		joinColumns = @JoinColumn(name = "RELEASE_VERSION_ID"),
		inverseJoinColumns = @JoinColumn(name = "APPLICATION_VERSIONS_ID"))
	@OrderBy("id")
	@JsonIgnore
	@ToString.Exclude
	private List<ApplicationVersion> applicationVersions = new ArrayList<>();

	@OneToMany(mappedBy = "releaseVersion", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@JsonIgnore
	@ToString.Exclude
	private List<ReleaseHistory> releaseHistories = new ArrayList<>();

	public ReleaseVersion(@NotNull String version) {
		this.version = version;
	}

	public ReleaseVersion(@NotNull Long id, @NotNull String version) {
		this.id = id;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		ReleaseVersion that = (ReleaseVersion) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
