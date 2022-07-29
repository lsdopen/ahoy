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
	private String name;
	private String version;
	private String dockerConfigJson;
	private Integer replicas;
	private Boolean routeEnabled;
	private List<ApplicationRouteValues> routes;
	private Boolean tls;
	private String tlsSecretName;
	private Boolean configFilesEnabled;
	private Map<String, ApplicationConfigFileValues> configFiles;
	private String configFileHashes;
	private String configPath;
	private Boolean volumesEnabled;
	private Map<String, ApplicationVolumeValues> volumes;
	private Boolean secretsEnabled;
	private Map<String, ApplicationSecretValues> secrets;
	private Map<String, ContainerValues> containers;
	private Map<String, ContainerValues> initContainers;
}
