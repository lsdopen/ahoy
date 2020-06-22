package za.co.lsd.ahoy.server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties("ahoy")
@Data
@Slf4j
public class AhoyServerProperties {
	private String repoPath;

	@PostConstruct
	public void logSummary() {
		log.info(toString());
	}
}
