package za.co.lsd.ahoy.server.cluster;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterRepository extends CrudRepository<Cluster, Long> {
}
