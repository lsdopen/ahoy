package za.co.lsd.ahoy.server.argocd.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoRepository {
	private String type;
	private String repo;
	@ToString.Exclude
	private boolean insecure;
	@ToString.Exclude
	private String username;
	@ToString.Exclude
	private String password;
	@ToString.Exclude
	private String sshPrivateKey;
	@EqualsAndHashCode.Exclude
	private ArgoConnectionState connectionState;
}
