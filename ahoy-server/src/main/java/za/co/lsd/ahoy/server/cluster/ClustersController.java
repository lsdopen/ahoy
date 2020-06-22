package za.co.lsd.ahoy.server.cluster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RepositoryRestController
@RequestMapping("/clusters")
@Slf4j
public class ClustersController {
	private final ClusterService clusterService;

	public ClustersController(ClusterService clusterService) {
		this.clusterService = clusterService;
	}

	@DeleteMapping("/destroy/{clusterId}")
	public ResponseEntity<?> destroy(@PathVariable Long clusterId) {
		Cluster cluster = clusterService.destroy(clusterId);

		return new ResponseEntity<>(cluster, new HttpHeaders(), HttpStatus.OK);
	}
}
