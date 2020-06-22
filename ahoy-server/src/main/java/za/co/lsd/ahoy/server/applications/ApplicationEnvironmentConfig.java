package za.co.lsd.ahoy.server.applications;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
public class ApplicationEnvironmentConfig {
	@EmbeddedId
	private ApplicationDeploymentId id;

	private Integer replicas;

	private String routeHostname;
	private Integer routeTargetPort;

	@ElementCollection
	private Map<String, String> environmentVariables;

	private String configFileName;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String configFileContent;

	public ApplicationEnvironmentConfig(ApplicationDeploymentId id, ApplicationEnvironmentConfig applicationEnvironmentConfig) {
		this.id = id;
		this.replicas = applicationEnvironmentConfig.getReplicas();
		this.routeHostname = applicationEnvironmentConfig.getRouteHostname();
		this.routeTargetPort = applicationEnvironmentConfig.getRouteTargetPort();
		this.environmentVariables = new LinkedHashMap<>(applicationEnvironmentConfig.getEnvironmentVariables());
		this.configFileName = applicationEnvironmentConfig.getConfigFileName();
		this.configFileContent = applicationEnvironmentConfig.getConfigFileContent();
	}

	public ApplicationEnvironmentConfig(String routeHostname, Integer routeTargetPort) {
		this.routeHostname = routeHostname;
		this.routeTargetPort = routeTargetPort;
	}
}
