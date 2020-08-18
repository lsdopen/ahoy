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

package za.co.lsd.ahoy.server.helm;

import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Path;

public class KubernetesTemplateWriter extends BaseTemplateWriter {

	@Override
	public void writeTemplatesImpl(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {

		copyBaseTemplates("/charts/kubernetes-helm/templates", templatesPath);

		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			addTemplate(application, "deployment", templatesPath);

			if (applicationVersion.hasConfigs()) {
				addTemplate(application, "configmap", templatesPath);
			}

			if (applicationVersion.hasVolumes()) {
				addTemplate(application, "pvc", templatesPath);
			}

			if (applicationVersion.hasSecrets()) {
				addTemplate(application, "secret-generic", templatesPath);
			}

			DockerRegistry dockerRegistry = applicationVersion.getDockerRegistry();
			if (dockerRegistry != null && dockerRegistry.getSecure()) {
				addTemplate(application, "secret-dockerconfig", templatesPath);
			}
			if (applicationVersion.getServicePorts() != null &&
				applicationVersion.getServicePorts().size() > 0) {
				addTemplate(application, "service", templatesPath);

				ApplicationEnvironmentConfig applicationEnvironmentConfig =
					environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)
						.orElse(null);
				if (applicationEnvironmentConfig != null && applicationEnvironmentConfig.getRouteHostname() != null && applicationEnvironmentConfig.getRouteTargetPort() != null) {
					addTemplate(application, "ingress", templatesPath);
				}
			}
		}
	}
}
