package za.co.lsd.ahoy.server.applications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDeploymentId implements Serializable {

	@Column(name = "environmentReleaseId")
	private EnvironmentReleaseId environmentReleaseId;

	@Column(name = "releaseVersionId")
	private Long releaseVersionId;

	@Column(name = "applicationVersionId")
	private Long applicationVersionId;
}
