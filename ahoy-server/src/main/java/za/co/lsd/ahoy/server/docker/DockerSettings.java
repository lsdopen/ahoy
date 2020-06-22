package za.co.lsd.ahoy.server.docker;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class DockerSettings {
	@Id
	private Long id;
	@OneToMany(mappedBy = "dockerSettings", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<DockerRegistry> dockerRegistries;
}
