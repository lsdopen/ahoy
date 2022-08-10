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
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ContainerSpec {
	protected ContainerType type = ContainerType.Container;
	protected String name = "default";
	protected String image;

	protected Boolean commandArgsEnabled;
	protected String command;
	protected List<String> args;

	protected Boolean servicePortsEnabled;
	protected List<Integer> servicePorts;

	protected Boolean healthChecksEnabled;

	protected ApplicationProbe livenessProbe;
	protected ApplicationProbe readinessProbe;

	protected Boolean environmentVariablesEnabled;
	protected List<ApplicationEnvironmentVariable> environmentVariables;

	protected Boolean resourcesEnabled;
	protected ApplicationResources resources;

	public boolean servicePortsEnabled() {
		return servicePortsEnabled != null && servicePortsEnabled;
	}

	public boolean hasServicePorts() {
		return servicePorts != null && servicePorts.size() > 0;
	}

	public boolean environmentVariablesEnabled() {
		return environmentVariablesEnabled != null && environmentVariablesEnabled;
	}

	public boolean hasEnvironmentVariables() {
		return environmentVariables != null && environmentVariables.size() > 0;
	}

	public boolean resourcesEnabled() {
		return resourcesEnabled != null && resourcesEnabled;
	}

	public boolean hasResources() {
		return resources != null;
	}

	public ContainerSpec(String name, String image) {
		this.name = name;
		this.image = image;
	}
}
