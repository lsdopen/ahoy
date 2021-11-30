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

package za.co.lsd.ahoy.server.environmentrelease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseHistory;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class EnvironmentRelease implements Serializable {

	@EmbeddedId
	private EnvironmentReleaseId id;

	@ManyToOne
	@MapsId("environmentId")
	@JoinColumn(name = "environmentId")
	private Environment environment;

	@ManyToOne
	@MapsId("releaseId")
	@JoinColumn(name = "releaseId")
	private Release release;

	@Enumerated(EnumType.STRING)
	private HealthStatus.StatusCode status;

	@JsonIgnore
	private String argoCdName;
	@JsonIgnore
	private String argoCdUid;

	private Integer applicationsReady;

	@OneToOne
	@JoinColumn(name = "currentReleaseVersionId")
	private ReleaseVersion currentReleaseVersion;

	@OneToOne
	@JoinColumn(name = "previousReleaseVersionId")
	private ReleaseVersion previousReleaseVersion;

	@OneToMany(mappedBy = "environmentRelease", cascade = CascadeType.REMOVE)
	@OrderBy("id")
	@JsonIgnore
	private List<ReleaseHistory> releaseHistories;

	public EnvironmentRelease(Environment environment, Release release) {
		this.environment = environment;
		this.release = release;
	}

	public EnvironmentRelease(EnvironmentReleaseId id, Environment environment, Release release) {
		this.id = id;
		this.environment = environment;
		this.release = release;
	}

	public String getNamespace() {
		return (release.getName() + "-" + environment.getName()).toLowerCase();
	}

	public boolean hasCurrentReleaseVersion() {
		return currentReleaseVersion != null;
	}

	public ReleaseVersion latestReleaseVersion() {
		List<ReleaseVersion> releaseVersions = release.getReleaseVersions();
		if (releaseVersions != null && releaseVersions.size() > 0) {
			ReleaseVersion latestReleaseVersion = releaseVersions.get(releaseVersions.size() - 1);
			if (hasCurrentReleaseVersion() && currentReleaseVersion.equals(latestReleaseVersion)) {
				return null;
			}
			return latestReleaseVersion;
		}
		return null;
	}

	@Override
	public String toString() {
		return "EnvironmentRelease{" +
			"id=" + id +
			", environment=" + environment +
			", release=" + release +
			", status=" + status +
			", applicationsReady=" + applicationsReady +
			", currentReleaseVersion=" + currentReleaseVersion +
			", argoCdName=" + argoCdName +
			", argoCdUid=" + argoCdUid +
			'}';
	}
}
