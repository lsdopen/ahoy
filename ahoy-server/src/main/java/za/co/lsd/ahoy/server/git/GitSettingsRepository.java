package za.co.lsd.ahoy.server.git;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "gitSettings", path = "gitSettings")
public interface GitSettingsRepository extends CrudRepository<GitSettings, Long> {
}
