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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Application {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	@Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$",
		message = "Name invalid: should start with and use lower case letters and numbers")
	private String name;

	@OneToMany(mappedBy = "application", cascade = CascadeType.REMOVE)
	@JsonIgnore
	@OrderBy("id")
	private List<ApplicationVersion> applicationVersions;

	public ApplicationVersion latestApplicationVersion() {
		if (applicationVersions != null && applicationVersions.size() > 0) {
			return applicationVersions.get(applicationVersions.size() - 1);
		}
		return null;
	}

	public Application(@NotNull String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Application{" + "id=" + id +
			", name='" + name + '\'' +
			", applicationVersions='" + applicationVersions + '\'' +
			'}';
	}
}
