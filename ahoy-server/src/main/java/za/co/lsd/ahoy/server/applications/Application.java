package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Application {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	private String name;

	@OneToMany(mappedBy = "application", cascade = CascadeType.REMOVE)
	@JsonIgnore
	private List<ApplicationVersion> applicationVersions;

	public ApplicationVersion latestApplicationVersion() {
		if (applicationVersions != null && applicationVersions.size() > 0) {
			return applicationVersions.get(applicationVersions.size() - 1);
		}
		return null;
	}

	public Application(@NotNull String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Application{" + "id=" + id +
			", name='" + name + '\'' +
			", applicationVersions='" + applicationVersions + '\'' +
			'}';
	}
}
