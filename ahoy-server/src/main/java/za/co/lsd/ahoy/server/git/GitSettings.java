package za.co.lsd.ahoy.server.git;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitSettings {
	@Id
	private Long id;
	private String remoteRepoUri;
	@Enumerated(EnumType.STRING)
	private Credentials credentials;
	@ToString.Exclude
	private String httpsUsername;
	@ToString.Exclude
	private String httpsPassword;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String privateKey;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String sshKnownHosts;

	public GitSettings(String remoteRepoUri) {
		this.remoteRepoUri = remoteRepoUri;
	}

	public enum Credentials {
		NONE,
		HTTPS,
		SSH
	}
}
