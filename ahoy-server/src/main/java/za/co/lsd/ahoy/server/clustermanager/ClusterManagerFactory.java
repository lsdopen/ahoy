/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
