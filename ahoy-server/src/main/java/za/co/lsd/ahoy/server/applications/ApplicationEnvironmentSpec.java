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
public class ApplicationEnvironmentSpec {
	private Integer replicas;

	private String routeHostname;
	private Integer routeTargetPort;
	private boolean tls;
	private String tlsSecretName;

	private Boolean environmentVariablesEnabled;
	private List<ApplicationEnvironmentVariable> environmentVariables;

	private List<ApplicationConfigFile> configFiles;

	private List<ApplicationVolume> volumes;

	private List<ApplicationSecret> secrets;

	private ApplicationResources resources;

	public ApplicationEnvironmentSpec(String routeHostname) {
		this.routeHostname = routeHostname;
	}

	public ApplicationEnvironmentSpec(String routeHostname, Integer routeTargetPort) {
		this.routeHostname = routeHostname;
		this.routeTargetPort = routeTargetPort;
	}
}
