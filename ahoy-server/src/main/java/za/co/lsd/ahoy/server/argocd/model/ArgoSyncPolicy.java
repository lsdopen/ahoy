package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoSyncPolicy {
	private Automated automated;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Automated {
		private Boolean prune;
		private Boolean selfHeal;
	}
}
