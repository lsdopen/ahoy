package za.co.lsd.ahoy.server.docker;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class DockerRegistry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String server;
	@ToString.Exclude
	private String username;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String password;
	@ToString.Exclude
	private Boolean secure;

	@ManyToOne
	@JsonBackReference
	@ToString.Exclude
	private DockerSettings dockerSettings;

	public DockerRegistry(String name, String server, String username, String password) {
		this.name = name;
		this.server = server;
		this.username = username;
		this.password = password;
		this.secure = true;
	}
}
