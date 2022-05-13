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

package za.co.lsd.ahoy.server.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import za.co.lsd.ahoy.server.environments.Environment;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Cluster implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull
	@Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$",
		message = "Name invalid: should start with and use lower case letters and numbers")
	private String name;
	@NotNull
	@JsonProperty(access = WRITE_ONLY)
	private String masterUrl;
	@NotNull
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@JsonProperty(access = WRITE_ONLY)
	@ToString.Exclude
	private String token;
	@NotNull
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@JsonProperty(access = WRITE_ONLY)
	@ToString.Exclude
	private String caCertData;
	@NotNull
	private String host;
	@NotNull
	private boolean inCluster;
	@NotNull
	@Enumerated(EnumType.STRING)
	private ClusterType type;

	@OneToMany(mappedBy = "cluster", cascade = CascadeType.REMOVE)
	@OrderBy("id")
	@JsonIgnore
	@ToString.Exclude
	private List<Environment> environments = new ArrayList<>();

	public Cluster(@NotNull String name, @NotNull String masterUrl, @NotNull ClusterType type) {
		this.name = name;
		this.masterUrl = masterUrl;
		this.type = type;
	}

	public Cluster(Long id, @NotNull String name, @NotNull String masterUrl, @NotNull ClusterType type) {
		this.id = id;
		this.name = name;
		this.masterUrl = masterUrl;
		this.type = type;
	}

	public Cluster(ClusterDTO dto) {
		this.id = dto.getId();
		this.name = dto.getName();
		this.masterUrl = dto.getMasterUrl();
		this.token = dto.getToken();
		this.caCertData = dto.getCaCertData();
		this.host = dto.getHost();
		this.inCluster = dto.isInCluster();
		this.type = dto.getType();
	}

	public void addEnvironment(Environment environment) {
		environments.add(environment);
		environment.setCluster(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Cluster cluster = (Cluster) o;
		return Objects.equals(id, cluster.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}