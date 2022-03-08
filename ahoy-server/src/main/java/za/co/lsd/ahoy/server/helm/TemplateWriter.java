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

package za.co.lsd.ahoy.server.helm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfigProvider;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.docker.DockerRegistry;
import za.co.lsd.ahoy.server.docker.DockerRegistryProvider;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static za.co.lsd.ahoy.server.helm.HelmUtils.*;

@Component
@Slf4j
public class TemplateWriter {
	public static final String HELM_TEMPLATES = "/charts/kubernetes-helm/templates";
	private final DockerRegistryProvider dockerRegistryProvider;
	private final ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private ResourcePatternResolver resourceResolver;

	public TemplateWriter(DockerRegistryProvider dockerRegistryProvider, ApplicationEnvironmentConfigProvider environmentConfigProvider) {
		this.dockerRegistryProvider = dockerRegistryProvider;
		this.environmentConfigProvider = environmentConfigProvider;
	}

	@Autowired
	public void setResourceResolver(ResourcePatternResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
	}

	public void writeTemplates(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {
		// TODO also protect that templates path also starts with repo dir
		if (!templatesPath.endsWith("templates"))
			throw new IllegalArgumentException("templatesPath does not seem to be a valid templates location");

		final List<Path> trackedTemplates = new ArrayList<>();
		copyBaseTemplates(templatesPath, trackedTemplates);

		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			addTemplate(application, "deployment", templatesPath, trackedTemplates);

			ApplicationEnvironmentConfig applicationEnvironmentConfig =
				environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)
					.orElse(null);

			if ((applicationVersion.configEnabled() && applicationVersion.hasConfigs()) ||
				(applicationEnvironmentConfig != null && applicationEnvironmentConfig.configEnabled() && applicationEnvironmentConfig.hasConfigs())) {
				addTemplate(application, "configmap", templatesPath, trackedTemplates);
			}

			if ((applicationVersion.volumesEnabled() && applicationVersion.hasVolumes()) ||
				(applicationEnvironmentConfig != null && applicationEnvironmentConfig.volumesEnabled() && applicationEnvironmentConfig.hasVolumes())) {
				addTemplate(application, "pvc", templatesPath, trackedTemplates);
			}

			if ((applicationVersion.secretsEnabled() && applicationVersion.hasSecrets()) ||
				(applicationEnvironmentConfig != null && applicationEnvironmentConfig.secretsEnabled() && applicationEnvironmentConfig.hasSecrets())) {
				addTemplate(application, "secret-generic", templatesPath, trackedTemplates);
			}

			Optional<DockerRegistry> dockerRegistry = dockerRegistryProvider.dockerRegistryFor(applicationVersion.getSpec().getDockerRegistryName());
			if (dockerRegistry.isPresent() && dockerRegistry.get().getSecure()) {
				addTemplate(application, "secret-dockerconfig", templatesPath, trackedTemplates);
			}

			if (applicationVersion.getSpec().getServicePortsEnabled() != null &&
				applicationVersion.getSpec().getServicePortsEnabled() &&
				applicationVersion.getSpec().getServicePorts() != null &&
				applicationVersion.getSpec().getServicePorts().size() > 0) {
				addTemplate(application, "service", templatesPath, trackedTemplates);

				if (applicationEnvironmentConfig != null &&
					applicationEnvironmentConfig.getSpec().getRouteEnabled() != null &&
					applicationEnvironmentConfig.getSpec().getRouteEnabled() &&
					applicationEnvironmentConfig.getSpec().getRouteHostname() != null && !applicationEnvironmentConfig.getSpec().getRouteHostname().trim().isEmpty() &&
					applicationEnvironmentConfig.getSpec().getRouteTargetPort() != null) {
					addTemplate(application, "ingress", templatesPath, trackedTemplates);
				}
			}
		}
		pruneTemplates(trackedTemplates, templatesPath);
	}

	private void copyBaseTemplates(Path outputTemplatesPath, List<Path> trackedTemplates) throws IOException {
		log.debug("Copying base templates from: {}", TemplateWriter.HELM_TEMPLATES);
		Resource[] resources = resourceResolver.getResources("classpath:" + TemplateWriter.HELM_TEMPLATES + "/*.yaml");
		for (Resource resource : resources) {
			Path templatePath = outputTemplatesPath.resolve(Objects.requireNonNull(resource.getFilename()));
			Files.copy(resource.getInputStream(), templatePath, StandardCopyOption.REPLACE_EXISTING);
			track(trackedTemplates, templatePath);
		}
	}

	private void addTemplate(Application application, String templateName, Path templatesPath, List<Path> trackedTemplates) throws IOException {
		String fileName = templateName + "-" + application.getName() + ".yaml";
		String data = "{{- $vals := dict \"glob\" .Values \"app\" .Values.applications." + valuesName(application) + " -}}\n" +
			"{{- include \"" + templateName + ".app\" $vals }}\n";

		Path templatePath = templatesPath.resolve(fileName);
		dump(data, templatePath);
		track(trackedTemplates, templatePath);

		log.debug("Added template '{}' for application '{}'", templateName, application.getName());
	}

	private void track(List<Path> trackedTemplates, Path templatePath) {
		trackedTemplates.add(templatePath);
	}

	private void pruneTemplates(List<Path> trackedTemplates, Path templatesPath) throws IOException {
		List<Path> extraTemplates = Files.list(templatesPath)
			.filter(path -> !trackedTemplates.contains(path))
			.collect(Collectors.toList());

		for (Path extraTemplate : extraTemplates) {
			log.debug("Deleting extra template file: {}", extraTemplate);
			Files.deleteIfExists(extraTemplate);
		}
	}
}
