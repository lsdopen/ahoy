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
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@NoArgsConstructor
public class ApplicationSecret {
	@NotNull
	private String name;
	@NotNull
	private SecretType type;

	@ToString.Exclude
	private Map<String, String> data;

	public ApplicationSecret(String name, SecretType type, Map<String, String> data) {
		this.name = name;
		this.type = type;
		this.data = data;
	}
}
