/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class ApplicationEnvironmentVariable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String key;
	@NotNull
	@Enumerated(EnumType.STRING)
	private EnvironmentVariableType type;

	// Value
	private String value;

	// Secret
	private String secretName;
	private String secretKey;

	@ManyToOne
	@JsonBackReference("applicationVersionReference")
	@ToString.Exclude
	private ApplicationVersion applicationVersion;

	@ManyToOne
	@JsonBackReference("applicationEnvironmentConfigReference")
	@ToString.Exclude
	private ApplicationEnvironmentConfig applicationEnvironmentConfig;

	public ApplicationEnvironmentVariable(String key, String value) {
		this.type = EnvironmentVariableType.Value;
		this.key = key;
		this.value = value;
	}

	public ApplicationEnvironmentVariable(String key, String secretName, String secretKey) {
		this.type = EnvironmentVariableType.Secret;
		this.key = key;
		this.secretName = secretName;
		this.secretKey = secretKey;
	}
}
