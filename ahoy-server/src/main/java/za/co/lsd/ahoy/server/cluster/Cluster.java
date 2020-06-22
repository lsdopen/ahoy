package za.co.lsd.ahoy.server.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import za.co.lsd.ahoy.server.environments.Environment;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Cluster {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name;
	@NotNull
	private String masterUrl;
	@NotNull
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String token;
	@NotNull
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String caCertData;
	@NotNull
	private String host;
	private String dockerRegistry;
	@NotNull
	private boolean inCluster;
	@NotNull
	@Enumerated(EnumType.STRING)
	private ClusterType type;

	@OneToMany(mappedBy = "cluster", cascade = CascadeType.REMOVE)
	@JsonIgnore
	private List<Environment> environments;

	public Cluster(@NotNull String name, @NotNull String masterUrl, @NotNull ClusterType type) {
		this.name = name;
		this.masterUrl = masterUrl;
		this.type = type;
	}

	public Cluster(Long id, @NotNull String name, @NotNull String masterUrl, @NotNull ClusterType type) {
		this.id = id;
		this.name = name;
		this.masterUrl = masterUrl;
		this.type = type;
	}

	@Override
	public String toString() {
		return "Cluster{" +
			"id=" + id +
			", name='" + name + '\'' +
			", masterUrl='" + masterUrl + '\'' +
			", type=" + type +
			'}';
	}
}
