package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class ApplicationConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String config;

	@ManyToOne
	@JsonBackReference
	private ApplicationVersion applicationVersion;

	public ApplicationConfig(@NotNull String name, String config) {
		this.name = name;
		this.config = config;
	}

	@Override
	public String toString() {
		return "ApplicationConfig{" + "id=" + id +
			", name='" + name + '\'' +
			'}';
	}
}
