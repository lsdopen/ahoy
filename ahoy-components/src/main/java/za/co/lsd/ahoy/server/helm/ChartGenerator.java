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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.helm.values.Values;
import za.co.lsd.ahoy.server.helm.values.ValuesBuilder;
import za.co.lsd.ahoy.server.release.ReleaseUtils;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Slf4j
public class ChartGenerator {
	private final ValuesBuilder valuesBuilder;
	private final TemplateWriter templateWriter;
	private ObjectFactory<Yaml> yamlObjectFactory;

	public ChartGenerator(ValuesBuilder valuesBuilder, TemplateWriter templateWriter) {
		this.valuesBuilder = valuesBuilder;
		this.templateWriter = templateWriter;
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
		log.trace("Writing chart: {}", chart);
		Path chartPath = releasePath.resolve("Chart.yaml");
		HelmUtils.dump(chart, chartPath, yaml);
	}

	private void writeTemplates(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {
		log.trace("Writing templates for {}, {} to {}", environmentRelease, releaseVersion, templatesPath);
		templateWriter.writeTemplates(environmentRelease, releaseVersion, templatesPath);
	}

	private void writeValues(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path releasePath, Yaml yaml) throws IOException {
		Values values = valuesBuilder.build(environmentRelease, releaseVersion);
		log.trace("Writing values: {}", values);
		Path valuesPath = releasePath.resolve("values.yaml");
		HelmUtils.dump(values, valuesPath, yaml);
	}
}
