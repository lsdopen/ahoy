package za.co.lsd.ahoy.server.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "release_version", columnNames = {"release_id", "version"}))
public class ReleaseVersion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String version;

	@ManyToOne
	private Release release;

	@ManyToMany
	@JsonIgnore
	private List<ApplicationVersion> applicationVersions;

	public ReleaseVersion(@NotNull String version, Release release, List<ApplicationVersion> applicationVersions) {
		this.version = version;
		this.release = release;
		this.applicationVersions = applicationVersions;
	}

	public ReleaseVersion(@NotNull Long id, @NotNull String version, Release release, List<ApplicationVersion> applicationVersions) {
		this.id = id;
		this.version = version;
		this.release = release;
		this.applicationVersions = applicationVersions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReleaseVersion that = (ReleaseVersion) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "ReleaseVersion{" +
			"id=" + id +
			", version='" + version + '\'' +
			'}';
	}
}
