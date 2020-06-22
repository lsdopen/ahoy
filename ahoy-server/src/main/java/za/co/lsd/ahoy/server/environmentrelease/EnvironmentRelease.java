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

	private String argoCdName;
	private String argoCdUid;

	private Integer applicationsReady;

	@OneToOne
	@JoinColumn(name = "currentReleaseVersionId")
	private ReleaseVersion currentReleaseVersion;

	@OneToOne
	@JoinColumn(name = "previousReleaseVersionId")
	private ReleaseVersion previousReleaseVersion;

	@OneToMany(mappedBy = "environmentRelease", cascade = CascadeType.REMOVE)
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
