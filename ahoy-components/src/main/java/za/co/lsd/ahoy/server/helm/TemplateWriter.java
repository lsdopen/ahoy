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
import za.co.lsd.ahoy.server.applications.*;
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
import java.util.stream.Stream;

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
		addTemplate("namespace", templatesPath, trackedTemplates);

		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			addTemplate(application, "deployment", templatesPath, trackedTemplates);

			Optional<ApplicationEnvironmentConfig> applicationEnvironmentConfig =
				environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion);

			ApplicationSpec applicationSpec = applicationVersion.getSpec();

			if ((applicationSpec.isConfigFilesEnabled() && applicationSpec.hasConfigs()) ||
				applicationEnvironmentConfig.map(e -> e.configEnabled() && e.hasConfigs()).orElse(false)) {
				addTemplate(application, "configmap", templatesPath, trackedTemplates);
			}

			if ((applicationSpec.isVolumesEnabled() && applicationSpec.hasVolumes()) ||
				applicationEnvironmentConfig.map(e -> e.volumesEnabled() && e.hasVolumes()).orElse(false)) {
				addTemplate(application, "pvc", templatesPath, trackedTemplates);
			}

			if ((applicationSpec.isSecretsEnabled() && applicationSpec.hasSecrets()) ||
				applicationEnvironmentConfig.map(e -> e.secretsEnabled() && e.hasSecrets()).orElse(false)) {
				addTemplate(application, "secret-generic", templatesPath, trackedTemplates);
			}

			Optional<DockerRegistry> dockerRegistry = dockerRegistryProvider.dockerRegistryFor(applicationSpec.getDockerRegistryName());
			if (dockerRegistry.isPresent() && dockerRegistry.get().isSecure()) {
				addTemplate(application, "secret-dockerconfig", templatesPath, trackedTemplates);
			}

			if (applicationSpec.allContainers().stream().anyMatch(
				(containerSpec -> containerSpec.servicePortsEnabled() && containerSpec.hasServicePorts()))) {
				addTemplate(application, "service", templatesPath, trackedTemplates);
			}

			if (applicationEnvironmentConfig.map(e -> e.routeEnabled() && e.hasRoutes()).orElse(false)) {
				addTemplate(application, "ingress", templatesPath, trackedTemplates);
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

	private void addTemplate(String templateName, Path templatesPath, List<Path> trackedTemplates) throws IOException {
		String fileName = templateName + ".yaml";

		Path templatePath = templatesPath.resolve(fileName);
		track(trackedTemplates, templatePath);

		log.debug("Added template '{}'", templateName);
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
		try (Stream<Path> templates = Files.list(templatesPath)) {
			List<Path> extraTemplates = templates.filter(path -> !trackedTemplates.contains(path)).collect(Collectors.toList());
			for (Path extraTemplate : extraTemplates) {
				log.debug("Deleting extra template file: {}", extraTemplate);
				Files.deleteIfExists(extraTemplate);
			}
		}
	}
}
