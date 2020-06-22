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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfigProvider;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static za.co.lsd.ahoy.server.helm.HelmUtils.*;

@Slf4j
public abstract class BaseTemplateWriter implements TemplateWriter {
	protected ApplicationEnvironmentConfigProvider environmentConfigProvider;
	protected ResourcePatternResolver resourceResolver;
	private List<Path> trackedTemplates = new ArrayList<>();

	@Autowired
	public void setEnvironmentConfigProvider(ApplicationEnvironmentConfigProvider environmentConfigProvider) {
		this.environmentConfigProvider = environmentConfigProvider;
	}

	@Autowired
	public void setResourceResolver(ResourcePatternResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
	}

	@Override
	public final void writeTemplates(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {
		// TODO also protect that templates path also starts with repo dir
		if (!templatesPath.endsWith("templates"))
			throw new IllegalArgumentException("templatesPath does not seem to be a valid templates location");

		this.writeTemplatesImpl(environmentRelease, releaseVersion, templatesPath);
		pruneTemplates(templatesPath);
	}

	protected abstract void writeTemplatesImpl(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException;

	protected void copyBaseTemplates(String baseTemplatesPath, Path outputTemplatesPath) throws IOException {
		log.debug("Copying base templates from: {}", baseTemplatesPath);
		Resource[] resources = resourceResolver.getResources("classpath:" + baseTemplatesPath + "/*.yaml");
		for (Resource resource : resources) {
			Path templatePath = outputTemplatesPath.resolve(Objects.requireNonNull(resource.getFilename()));
			Files.copy(resource.getInputStream(), templatePath, StandardCopyOption.REPLACE_EXISTING);
			track(templatePath);
		}
	}

	protected void addTemplate(Application application, String templateName, Path templatesPath) throws IOException {
		String fileName = templateName + "-" + application.getName() + ".yaml";
		String data = "{{- $vals := dict \"glob\" .Values \"app\" .Values.applications." + valuesName(application) + " -}}\n" +
			"{{- include \"" + templateName + ".app\" $vals }}\n";

		Path templatePath = templatesPath.resolve(fileName);
		dump(data, templatePath);
		track(templatePath);

		log.debug("Added template '{}' for application '{}'", templateName, application.getName());
	}

	private void track(Path templatePath) {
		trackedTemplates.add(templatePath);
	}

	private void pruneTemplates(Path templatesPath) throws IOException {
		List<Path> extraTemplates = Files.list(templatesPath)
			.filter(path -> !trackedTemplates.contains(path))
			.collect(Collectors.toList());

		for (Path extraTemplate : extraTemplates) {
			log.debug("Deleting extra template file: {}", extraTemplate);
			Files.deleteIfExists(extraTemplate);
		}
	}
}
