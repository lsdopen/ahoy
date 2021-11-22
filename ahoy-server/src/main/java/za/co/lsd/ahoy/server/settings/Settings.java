/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
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
}
