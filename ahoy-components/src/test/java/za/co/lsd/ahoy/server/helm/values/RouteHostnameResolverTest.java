/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.helm.values;

import org.junit.jupiter.api.Test;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterType;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.releases.Release;

import static org.junit.jupiter.api.Assertions.*;

class RouteHostnameResolverTest {
	private final RouteHostnameResolver resolver = new RouteHostnameResolver();

	@Test
	void resolve() {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setHost("minikube.host");
		Environment environment = new Environment("dev");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		Application application = new Application("app1");
		String routeHostname = "${release_name}-${application_name}-${environment_name}.${cluster_host}";

		// when
		String resolvedRouteHostname = resolver.resolve(environmentRelease, application, routeHostname);

		// then
		assertEquals("release1-app1-dev.minikube.host", resolvedRouteHostname);
	}

	@Test
	void resolveNull() {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setHost("minikube.host");
		Environment environment = new Environment("dev");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		Application application = new Application("app1");
		String routeHostname = null;

		// when
		String resolvedRouteHostname = resolver.resolve(environmentRelease, application, routeHostname);

		// then
		assertNull(resolvedRouteHostname);
	}

	@Test
	void resolveIncorrectKey() {
		// given
		Cluster cluster = new Cluster("test-cluster", "https://kubernetes.default.svc", ClusterType.KUBERNETES);
		cluster.setHost("minikube.host");
		Environment environment = new Environment("dev");
		cluster.addEnvironment(environment);
		Release release = new Release("release1");
		EnvironmentRelease environmentRelease = new EnvironmentRelease(environment, release);
		Application application = new Application("app1");
		String routeHostname = "${releaseXX_name}-${application_name}-${environment_name}.${cluster_host}";

		// when
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			resolver.resolve(environmentRelease, application, routeHostname);
		});

		// then
		assertEquals("Failed to resolve route hostname for app1: ${releaseXX_name}-${application_name}-${environment_name}.${cluster_host}, reason: Cannot resolve variable 'releaseXX_name' (enableSubstitutionInVariables=false).",
			exception.getMessage());
	}
}
