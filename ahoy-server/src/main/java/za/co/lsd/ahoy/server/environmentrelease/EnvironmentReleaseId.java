package za.co.lsd.ahoy.server.environmentrelease;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentReleaseId implements Serializable {

	@Column(name = "environmentId")
	private Long environmentId;

	@Column(name = "releaseId")
	private Long releaseId;
}
