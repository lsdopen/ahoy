package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoApplicationWatchEvent {
	private Result result;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Result {
		private String type;
		private ArgoApplication application;
	}
}
