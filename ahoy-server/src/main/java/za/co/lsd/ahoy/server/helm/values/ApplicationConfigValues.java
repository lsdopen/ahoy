package za.co.lsd.ahoy.server.helm.values;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationConfig;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationConfigValues {
	public String name;
	public String config;

	public ApplicationConfigValues(ApplicationConfig config) {
		this.name = config.getName();
		this.config = config.getConfig();
	}

	public ApplicationConfigValues(ApplicationEnvironmentConfig config) {
		this.name = config.getConfigFileName();
		this.config = config.getConfigFileContent();
	}
}
