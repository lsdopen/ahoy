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

package za.co.lsd.ahoy.server.environments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Environment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name;

	@ManyToOne
	@JoinColumn
	private Cluster cluster;

	@OneToMany(mappedBy = "environment", cascade = CascadeType.REMOVE)
	@JsonIgnore
	private List<EnvironmentRelease> environmentReleases;

	public Environment(@NotNull String name, Cluster cluster) {
		this.name = name;
		this.cluster = cluster;
	}

	public Environment(Long id, @NotNull String name, Cluster cluster) {
		this.id = id;
		this.name = name;
		this.cluster = cluster;
	}

	@Override
	public String toString() {
		return "Environment{" + "id=" + id +
			", name='" + name + '\'' +
			", cluster='" + cluster + '\'' +
			'}';
	}
}
