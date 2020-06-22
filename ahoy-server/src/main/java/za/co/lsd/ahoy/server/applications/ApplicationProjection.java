package za.co.lsd.ahoy.server.applications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.List;

@Projection(name = "application", types = {Application.class})
public interface ApplicationProjection {
	@Value("#{target.id}")
	long getId();

	@Value("#{target.name}")
	String getName();

	@Value("#{target.applicationVersions}")
	List<ApplicationVersion> getApplicationVersions();

	@Value("#{target.latestApplicationVersion()}")
	ApplicationVersion getLatestApplicationVersion();
}
