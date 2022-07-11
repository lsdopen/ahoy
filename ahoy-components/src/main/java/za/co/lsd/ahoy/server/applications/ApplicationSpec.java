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

package za.co.lsd.ahoy.server.applications;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ApplicationSpec extends ContainerSpec {
	private String dockerRegistryName;

	private boolean configFilesEnabled;
	private String configPath;
	private List<ApplicationConfigFile> configFiles;

	private boolean volumesEnabled;
	private List<ApplicationVolume> volumes;

	private boolean secretsEnabled;
	private List<ApplicationSecret> secrets;

	private List<ContainerSpec> containers = new ArrayList<>();

	public ApplicationSpec(String name, String image, String dockerRegistryName) {
		this.name = name;
		this.image = image;
		this.dockerRegistryName = dockerRegistryName;
	}

	public boolean hasConfigs() {
		return configFiles != null && configFiles.size() > 0;
	}

	public boolean hasVolumes() {
		return volumes != null && volumes.size() > 0;
	}

	public boolean hasSecrets() {
		return secrets != null && secrets.size() > 0;
	}

	/**
	 * Returns a list including the default container spec; i.e. this application spec, and all its children container specs.
	 *
	 * @return list of container specs
	 */
	public List<ContainerSpec> allContainers() {
		List<ContainerSpec> containerSpecs = new ArrayList<>();
		containerSpecs.add(this);
		if (containers != null)
			containerSpecs.addAll(containers);
		return containerSpecs;
	}
}
