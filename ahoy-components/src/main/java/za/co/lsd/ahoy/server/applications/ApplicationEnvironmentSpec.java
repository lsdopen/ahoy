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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class ApplicationEnvironmentSpec {
	private Integer replicas;

	private boolean routeEnabled;
	private List<ApplicationRoute> routes = new ArrayList<>();

	/**
	 * @deprecated no longer used in favout of list of routes
	 */
	@Deprecated(since = "0.16.0", forRemoval = true)
	@JsonIgnore
	private String routeHostname;
	/**
	 * @deprecated no longer used in favout of list of routes
	 */
	@Deprecated(since = "0.16.0", forRemoval = true)
	@JsonIgnore
	private Integer routeTargetPort;

	private boolean tls;
	private String tlsSecretName;

	private boolean environmentVariablesEnabled;
	private List<ApplicationEnvironmentVariable> environmentVariables;

	private boolean configFilesEnabled;
	private List<ApplicationConfigFile> configFiles;

	private boolean volumesEnabled;
	private List<ApplicationVolume> volumes;

	private boolean secretsEnabled;
	private List<ApplicationSecret> secrets;

	private boolean resourcesEnabled;
	private ApplicationResources resources;

	public static ApplicationEnvironmentSpec newSummarySpec(Boolean routeEnabled, List<ApplicationRoute> routes) {
		ApplicationEnvironmentSpec newSummarySpec = new ApplicationEnvironmentSpec();
		newSummarySpec.setRouteEnabled(routeEnabled);
		newSummarySpec.setRoutes(routes);
		return newSummarySpec;
	}

	public ApplicationEnvironmentSpec(String routeHostname, Integer routeTargetPort) {
		this.routeEnabled = routeHostname != null && routeTargetPort != null;
		this.routes = Collections.singletonList(new ApplicationRoute(routeHostname, routeTargetPort));
	}
}
