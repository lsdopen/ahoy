package za.co.lsd.ahoy.server.argocd;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "argoSettings", path = "argoSettings")
public interface ArgoSettingsRepository extends CrudRepository<ArgoSettings, Long> {
}
