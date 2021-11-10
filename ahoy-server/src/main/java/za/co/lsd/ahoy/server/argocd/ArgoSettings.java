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

package za.co.lsd.ahoy.server.argocd;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;
import za.co.lsd.ahoy.server.settings.BaseSettings;
import za.co.lsd.ahoy.server.settings.Settings;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ArgoSettings extends BaseSettings {
	private String argoServer;
	@ToString.Exclude
	private String argoToken;

	public ArgoSettings() {
		super(Settings.Type.ARGO);
	}

	public boolean configured() {
		return StringUtils.hasText(argoServer);
	}
}
