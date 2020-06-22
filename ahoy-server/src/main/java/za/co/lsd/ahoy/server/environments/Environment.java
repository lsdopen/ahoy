package za.co.lsd.ahoy.server.environments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Environment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name;

	@ManyToOne
	@JoinColumn
	private Cluster cluster;

	@OneToMany(mappedBy = "environment", cascade = CascadeType.REMOVE)
	@JsonIgnore
	private List<EnvironmentRelease> environmentReleases;

	public Environment(@NotNull String name, Cluster cluster) {
		this.name = name;
		this.cluster = cluster;
	}

	public Environment(Long id, @NotNull String name, Cluster cluster) {
		this.id = id;
		this.name = name;
		this.cluster = cluster;
	}

	@Override
	public String toString() {
		return "Environment{" + "id=" + id +
			", name='" + name + '\'' +
			", cluster='" + cluster + '\'' +
			'}';
	}
}
