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

package za.co.lsd.ahoy.server.settings;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class Settings {
	@Id
	@Enumerated(EnumType.STRING)
	private Type type;
	@NotNull
	@Convert(converter = SettingsConverter.class)
	@Column(length = 10485760)
	private BaseSettings settings;

	public Settings(BaseSettings settings) {
		this.type = settings.getType();
		this.settings = settings;
	}

	@Getter
	public enum Type {
		GIT,
		ARGO,
		DOCKER;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Settings settings = (Settings) o;
		return Objects.equals(type, settings.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}
}
