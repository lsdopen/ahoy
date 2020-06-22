package za.co.lsd.ahoy.server.clustermanager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.cluster.Cluster;

@Component
public class ClusterManagerFactory {
	private final ObjectProvider<OpenShiftClusterManager> openshiftClusterManagerFactory;
	private final ObjectProvider<KubernetesClusterManager> kubernetesClusterManagerFactory;
	private final ObjectProvider<NoOpClusterManager> noOpClusterManagerFactory;

	public ClusterManagerFactory(ObjectProvider<OpenShiftClusterManager> openshiftClusterManagerFactory, ObjectProvider<KubernetesClusterManager> kubernetesClusterManagerFactory, ObjectProvider<NoOpClusterManager> noOpClusterManagerFactory) {
		this.openshiftClusterManagerFactory = openshiftClusterManagerFactory;
		this.kubernetesClusterManagerFactory = kubernetesClusterManagerFactory;
		this.noOpClusterManagerFactory = noOpClusterManagerFactory;
	}

	public ClusterManager newManager(Cluster cluster) {
		switch (cluster.getType()) {
			case OPENSHIFT:
				return openshiftClusterManagerFactory.getObject(cluster);
			case KUBERNETES:
				return kubernetesClusterManagerFactory.getObject(cluster);
			case NOOP:
				return noOpClusterManagerFactory.getObject(cluster);
			default:
				throw new IllegalArgumentException("Failed to create cluster manager. Cluster type unknown:" + cluster.getType());
		}
	}
}
