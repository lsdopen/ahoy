package za.co.lsd.ahoy.server.releases;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.applications.ApplicationVersionProjection;

import java.util.List;

@Projection(name = "releaseVersion", types = {ReleaseVersion.class})
public interface ReleaseVersionProjection {
	@Value("#{target.id}")
	long getId();

	@Value("#{target.version}")
	String getVersion();

	@Value("#{target.applicationVersions}")
	List<ApplicationVersionProjection> getApplicationVersions();
}
