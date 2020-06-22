package za.co.lsd.ahoy.server.helm.values;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Values {
	public String host;
	public String dockerRegistry;
	public String imageNamespace;
	public String environment;
	public String releaseName;
	public String releaseVersion;
	public Map<String, ApplicationValues> applications;
}
