package za.co.lsd.ahoy.server.releases;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.List;

@Projection(name = "release", types = {Release.class})
public interface ReleaseProjection {
	@Value("#{target.id}")
	long getId();

	@Value("#{target.name}")
	String getName();

	@Value("#{target.releaseVersions}")
	List<ReleaseVersionProjection> getReleaseVersions();
}
