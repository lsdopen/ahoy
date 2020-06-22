package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoMetadata {
	public static final String RELEASE_VERSION_LABEL = "releaseVersion";

	private String name;
	private String uid;
	private Map<String, String> labels;
}
