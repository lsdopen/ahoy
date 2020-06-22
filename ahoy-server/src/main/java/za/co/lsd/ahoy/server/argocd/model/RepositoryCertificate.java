package za.co.lsd.ahoy.server.argocd.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryCertificate {
	@ToString.Exclude
	private String certData;
	@ToString.Exclude
	private String certInfo;
	@ToString.Exclude
	private String certSubType;
	@ToString.Exclude
	private String certType;
	private String serverName;
}
