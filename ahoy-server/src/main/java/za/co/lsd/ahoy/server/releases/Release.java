package za.co.lsd.ahoy.server.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Release {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	private String name;

	@OneToMany(mappedBy = "release")
	@JsonIgnore
	private List<EnvironmentRelease> environmentReleases;

	@OneToMany(mappedBy = "release")
	@JsonIgnore
	private List<ReleaseVersion> releaseVersions;

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
