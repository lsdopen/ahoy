/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationValues {
	public String name;
	public String image;
	public String dockerConfigJson;
	public String version;
	public List<Integer> servicePorts;
	public Integer replicas;
	public String routeHostname;
	public Integer routeTargetPort;
	public boolean tls;
	public String tlsSecretName;
	public Map<String, EnvironmentVariableValues> environmentVariables;
	public String healthEndpointPath;
	public Integer healthEndpointPort;
	public String healthEndpointScheme;
	public Map<String, ApplicationConfigFileValues> configFiles;
	public String configPath;
	public Map<String, ApplicationVolumeValues> volumes;
	public Map<String, ApplicationSecretValues> secrets;
}
