package za.co.lsd.ahoy.server.applications;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;

import java.io.Serializable;

public class ApplicationDeploymentIdConverters {

	@Component
	public static class IdToStringConverter implements Converter<ApplicationDeploymentId, String> {

		@Override
		public String convert(ApplicationDeploymentId id) {
			EnvironmentReleaseId environmentReleaseId = id.getEnvironmentReleaseId();
			return String.format("%s_%s_%s_%s",
				environmentReleaseId.getEnvironmentId(),
				environmentReleaseId.getReleaseId(),
				id.getReleaseVersionId(),
				id.getApplicationVersionId());
		}
	}

	@Component
	public static class StringToIdConverter implements Converter<String, ApplicationDeploymentId> {

		@Override
		public ApplicationDeploymentId convert(String id) {
			String[] parts = id.split("_");
			if (parts.length != 4)
				throw new IllegalArgumentException("Invalid request id; require 4 parts: " + id);

			return new ApplicationDeploymentId(
				new EnvironmentReleaseId(Long.parseLong(parts[0]), Long.parseLong(parts[1])),
				Long.parseLong(parts[2]), Long.parseLong(parts[3]));
		}
	}

	@Component
	public static class IdConverter implements BackendIdConverter {
		private final Converter<String, ApplicationDeploymentId> stringToIdConverter;
		private final Converter<ApplicationDeploymentId, String> idToStringConverter;

		public IdConverter(Converter<String, ApplicationDeploymentId> stringToIdConverter, Converter<ApplicationDeploymentId, String> idToStringConverter) {
			this.stringToIdConverter = stringToIdConverter;
			this.idToStringConverter = idToStringConverter;
		}

		@Override
		public Serializable fromRequestId(String id, Class<?> entityType) {
			if (id == null)
				return null;

			return stringToIdConverter.convert(id);
		}

		@Override
		public String toRequestId(Serializable source, Class<?> entityType) {
			return idToStringConverter.convert((ApplicationDeploymentId) source);
		}

		@Override
		public boolean supports(Class<?> type) {
			return ApplicationEnvironmentConfig.class.equals(type) || ApplicationDeploymentId.class.equals(type);
		}
	}
}
