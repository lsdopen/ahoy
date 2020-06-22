package za.co.lsd.ahoy.server.docker;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "dockerSettings", path = "dockerSettings")
public interface DockerSettingsRepository extends CrudRepository<DockerSettings, Long> {
}
