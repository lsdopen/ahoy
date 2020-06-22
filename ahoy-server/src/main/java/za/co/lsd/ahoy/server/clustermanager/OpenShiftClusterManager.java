package za.co.lsd.ahoy.server.clustermanager;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environments.Environment;

@Component
@Scope("prototype")
@Slf4j
public class OpenShiftClusterManager implements ClusterManager {
	private final Config config;

	public OpenShiftClusterManager(Cluster cluster) {
		config = new ConfigBuilder()
			.withMasterUrl(cluster.getMasterUrl())
			.withOauthToken(cluster.getToken())
			.build();
	}

	@Override
	public void createEnvironment(Environment environment) {
		log.debug("Creating project for environment {}", environment);

		try (DefaultOpenShiftClient openShiftClient = new DefaultOpenShiftClient(config)) {

			openShiftClient.projects().createOrReplaceWithNew()
				.withNewMetadata()
				.withName(environment.getName())
				.endMetadata()
				.done();

			log.debug("Project created for environment {}", environment);

		} catch (Throwable e) {
			log.error("Failed to create environment " + environment, e);
			throw new ClusterManagerException("Failed to create environment", e);
		}
	}

	@Override
	public void deleteEnvironment(Environment environment) {
		log.debug("Deleting project for environment {}", environment);

		try (DefaultOpenShiftClient openShiftClient = new DefaultOpenShiftClient(config)) {

			if (openShiftClient.projects()
				.withName(environment.getName())
				.delete()) {
				log.debug("Project deleted for environment {}", environment);
			}

		} catch (Throwable e) {
			log.error("Failed to delete environment " + environment, e);
			throw new ClusterManagerException("Failed to delete environment", e);
		}
	}
}
