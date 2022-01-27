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

package za.co.lsd.ahoy.server.releases;

import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import za.co.lsd.ahoy.server.environments.Environment;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class ReleaseHistory implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private Environment environment;

	@ManyToOne
	private Release release;

	@ManyToOne
	private ReleaseVersion releaseVersion;

	@NotNull
	@Enumerated(EnumType.STRING)
	private ReleaseHistoryAction action;
	@NotNull
	@Enumerated(EnumType.STRING)
	private ReleaseHistoryStatus status;
	@NotNull
	private LocalDateTime time;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		ReleaseHistory that = (ReleaseHistory) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
