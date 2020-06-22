package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {
	private String message;
	private StatusCode status;

	public enum StatusCode {
		Unknown,
		Progressing,
		Healthy,
		Suspended,
		Degraded,
		Missing
	}
}
