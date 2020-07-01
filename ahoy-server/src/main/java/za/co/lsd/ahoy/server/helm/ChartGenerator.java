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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import za.co.lsd.ahoy.server.ReleaseUtils;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfig;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentConfigProvider;
import za.co.lsd.ahoy.server.applications.ApplicationVersion;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.helm.values.ApplicationValues;
import za.co.lsd.ahoy.server.helm.values.Values;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ChartGenerator {
	private ObjectProvider<TemplateWriter> templateWriterFactory;
	private ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer;
	private ObjectFactory<Yaml> yamlObjectFactory;

	@Autowired
	public void setTemplateWriterFactory(ObjectProvider<TemplateWriter> templateWriterFactory) {
		this.templateWriterFactory = templateWriterFactory;
	}

	@Autowired
	public void setEnvironmentConfigProvider(ApplicationEnvironmentConfigProvider environmentConfigProvider) {
		this.environmentConfigProvider = environmentConfigProvider;
	}

	@Autowired
	public void setDockerConfigSealedSecretProducer(DockerConfigSealedSecretProducer dockerConfigSealedSecretProducer) {
		this.dockerConfigSealedSecretProducer = dockerConfigSealedSecretProducer;
	}

	@Autowired
	public void setYamlObjectFactory(ObjectFactory<Yaml> yamlObjectFactory) {
		this.yamlObjectFactory = yamlObjectFactory;
	}

	public void generate(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path path) throws IOException {
		Objects.requireNonNull(environmentRelease, "environmentRelease is required");
		Objects.requireNonNull(releaseVersion, "releaseVersion is required");

		Path releasePath = path.resolve(ReleaseUtils.resolveReleasePath(environmentRelease));
		if (!Files.exists(releasePath))
			Files.createDirectories(releasePath);

		Path templatesPath = releasePath.resolve("templates");
		if (!Files.exists(templatesPath))
			Files.createDirectories(templatesPath);

		Yaml yaml = yamlObjectFactory.getObject();

		writeChart(environmentRelease, releaseVersion, releasePath, yaml);
		writeTemplates(environmentRelease, releaseVersion, templatesPath);
		writeValues(environmentRelease, releaseVersion, releasePath, yaml);
	}

	private void writeChart(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path releasePath, Yaml yaml) throws IOException {
		Chart chart = Chart.builder()
			.name(environmentRelease.getRelease().getName())
			.version(releaseVersion.getVersion())
			.build();
		log.debug("Writing chart: {}", chart);
		Path chartPath = releasePath.resolve("Chart.yaml");
		HelmUtils.dump(chart, chartPath, yaml);
	}

	private void writeTemplates(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {
		log.debug("Writing templates for {}, {} to {}", environmentRelease, releaseVersion, templatesPath);
		TemplateWriter templateWriter = templateWriterFactory.getObject(environmentRelease.getEnvironment().getCluster().getType());
		templateWriter.writeTemplates(environmentRelease, releaseVersion, templatesPath);
	}

	private void writeValues(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path releasePath, Yaml yaml) throws IOException {
		Environment environment = environmentRelease.getEnvironment();
		Cluster cluster = environment.getCluster();

		Values.ValuesBuilder valuesBuilder = Values.builder()
			.host(cluster.getHost())
			.environment(environment.getName())
			.releaseName(releaseVersion.getRelease().getName())
			.releaseVersion(releaseVersion.getVersion());

		Map<String, ApplicationValues> apps = new LinkedHashMap<>();
		for (ApplicationVersion applicationVersion : releaseVersion.getApplicationVersions()) {
			Application application = applicationVersion.getApplication();
			ApplicationEnvironmentConfig applicationEnvironmentConfig =
				environmentConfigProvider.environmentConfigFor(environmentRelease, releaseVersion, applicationVersion)
					.orElse(null);
			apps.put(HelmUtils.valuesName(application), ApplicationValues.build(applicationVersion, applicationEnvironmentConfig, dockerConfigSealedSecretProducer));

			log.debug("Added values for application '{}' in environment '{}'", application.getName(), environment.getName());
		}
		valuesBuilder.applications(apps);

		Values values = valuesBuilder.build();
		log.debug("Writing values: {}", values);
		Path valuesPath = releasePath.resolve("values.yaml");
		HelmUtils.dump(values, valuesPath, yaml);
	}
}
