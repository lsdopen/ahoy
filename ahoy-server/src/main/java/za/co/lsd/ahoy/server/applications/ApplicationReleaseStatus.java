package za.co.lsd.ahoy.server.applications;

import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
@Data
@NoArgsConstructor
public class ApplicationReleaseStatus {
	@EmbeddedId
	private ApplicationDeploymentId id;

	@Enumerated(EnumType.STRING)
	private HealthStatus.StatusCode status;

	public ApplicationReleaseStatus(ApplicationDeploymentId id) {
		this.id = id;
	}
}
