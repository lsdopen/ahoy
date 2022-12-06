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

package za.co.lsd.ahoy.server.helm.values;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.applications.Application;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class RouteHostnameResolver {

	public String resolve(EnvironmentRelease environmentRelease, Application application, String routeHostname) {
		if (routeHostname == null)
			return null;

		Objects.requireNonNull(environmentRelease, "environmentRelease is required");
		Objects.requireNonNull(application, "application is required");

		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("cluster_host", environmentRelease.getEnvironment().getCluster().getHost());
		valuesMap.put("environment_key", environmentRelease.getEnvironment().getKey());
		valuesMap.put("release_name", environmentRelease.getRelease().getName());
		valuesMap.put("application_name", application.getName());

		StringSubstitutor stringSubstitutor = new StringSubstitutor(valuesMap);
		stringSubstitutor.setEnableUndefinedVariableException(true);

		try {
			return stringSubstitutor.replace(routeHostname);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to resolve route hostname for " +
				application.getName() +
				": " + routeHostname +
				", reason: " + e.getMessage());
		}
	}
}
