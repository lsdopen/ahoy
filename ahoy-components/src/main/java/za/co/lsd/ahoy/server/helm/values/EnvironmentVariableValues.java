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
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationEnvironmentVariable;
import za.co.lsd.ahoy.server.applications.EnvironmentVariableType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentVariableValues {
	private String key;
	private String type;
	private String value;
	private String secretName;
	private String secretKey;

	public EnvironmentVariableValues(ApplicationEnvironmentVariable environmentVariable) {
		this.key = environmentVariable.getKey();
		this.type = environmentVariable.getType().name();

		if (environmentVariable.getType().equals(EnvironmentVariableType.Value)) {
			this.value = environmentVariable.getValue();

		} else if (environmentVariable.getType().equals(EnvironmentVariableType.Secret)) {
			this.secretName = environmentVariable.getSecretName();
			this.secretKey = environmentVariable.getSecretKey();
		}
	}

	public static EnvironmentVariableValues createValues(String key, String value) {
		return new EnvironmentVariableValues(key, EnvironmentVariableType.Value.name(), value, null, null);
	}

	public static EnvironmentVariableValues createSecretValues(String key, String secretName, String secretKey) {
		return new EnvironmentVariableValues(key, EnvironmentVariableType.Secret.name(), null, secretName, secretKey);
	}
}
