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
import za.co.lsd.ahoy.server.applications.ApplicationProbe;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerValues {
	public String name;
	public String image;
	public Boolean commandArgsEnabled;
	public String command;
	public List<String> args;
	public Boolean servicePortsEnabled;
	public List<Integer> servicePorts;
	public Boolean environmentVariablesEnabled;
	public Map<String, EnvironmentVariableValues> environmentVariables;
	public Boolean healthChecksEnabled;
	public ApplicationProbe livenessProbe;
	public ApplicationProbe readinessProbe;
	public Boolean resourcesEnabled;
	public ResourcesValues resources;
}