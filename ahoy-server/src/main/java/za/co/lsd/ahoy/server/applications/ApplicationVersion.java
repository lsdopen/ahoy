package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.util.IntegerListConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "application_version", columnNames = {"application_id", "version"}))
public class ApplicationVersion {
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

	@ElementCollection
	private Map<String, String> environmentVariables;

	private String healthEndpointPath;
	private Integer healthEndpointPort;
	private String healthEndpointScheme;

	@ManyToOne
	private Application application;

	private String configPath;

	@OneToMany(mappedBy = "applicationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<ApplicationConfig> configs;

	public ApplicationVersion(@NotNull String version, @NotNull String image, Application application) {
		this.version = version;
		this.image = image;
		this.application = application;
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
