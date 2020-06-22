package za.co.lsd.ahoy.server.helm;

import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Path;

public class OpenShiftTemplateWriter extends BaseTemplateWriter {

	@Override
	public void writeTemplatesImpl(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {

		copyBaseTemplates("/charts/openshift-helm/templates", templatesPath);

		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			addTemplate(application, "imagestream", templatesPath);
			addTemplate(application, "configmap", templatesPath);
			addTemplate(application, "deployment", templatesPath);
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
					addTemplate(application, "route", templatesPath);
				}
			}
		}
	}
}
