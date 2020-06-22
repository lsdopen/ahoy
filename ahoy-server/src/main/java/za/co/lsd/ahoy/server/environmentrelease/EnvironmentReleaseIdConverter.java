package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class EnvironmentReleaseIdConverter implements BackendIdConverter {

	@Override
	public Serializable fromRequestId(String id, Class<?> entityType) {
		if (id == null)
			return null;

		String[] parts = id.split("_");
		return new EnvironmentReleaseId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
	}

	@Override
	public String toRequestId(Serializable source, Class<?> entityType) {
		EnvironmentReleaseId id = (EnvironmentReleaseId) source;
		return String.format("%s_%s", id.getEnvironmentId(), id.getReleaseId());
	}

	@Override
	public boolean supports(Class<?> type) {
		return EnvironmentRelease.class.equals(type) || EnvironmentReleaseId.class.equals(type);
	}
}
