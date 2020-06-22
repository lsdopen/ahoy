package za.co.lsd.ahoy.server;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeployDetails {
	private String commitMessage;

	public DeployDetails(String commitMessage) {
		this.commitMessage = commitMessage;
	}
}
