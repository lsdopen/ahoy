package za.co.lsd.ahoy.server.cluster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class InitDefaultCluster {
	private final ClusterRepository clusterRepository;

	public InitDefaultCluster(ClusterRepository clusterRepository) {
		this.clusterRepository = clusterRepository;
	}

	@PostConstruct
	public void init() throws IOException {
		if (clusterRepository.count() == 0) {
			log.info("Detected no clusters setup");
			Path token = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token");
			Path caCrt = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

			if (Files.exists(token) && Files.exists(caCrt)) {
				log.info("Found token and ca.crt files");
				Cluster cluster = new Cluster("in-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
				cluster.setHost("minikube.host");
				cluster.setToken(Files.readString(token));
				cluster.setCaCertData(Files.readString(caCrt));
				cluster.setInCluster(true);
				log.info("Saving default cluster: {}", cluster);
				clusterRepository.save(cluster);
			}
		}
	}
}
