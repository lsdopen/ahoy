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

package za.co.lsd.ahoy.server.git;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import za.co.lsd.ahoy.server.settings.BaseSettings;
import za.co.lsd.ahoy.server.settings.Settings;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class GitSettings extends BaseSettings {
	private String remoteRepoUri;
	private String branch = "master";
	private Credentials credentials;
	@ToString.Exclude
	private String httpsUsername;
	@ToString.Exclude
	private String httpsPassword;
	@ToString.Exclude
	private String privateKey;
	@ToString.Exclude
	private String sshKnownHosts;

	public GitSettings() {
		super(Settings.Type.GIT);
	}

	public GitSettings(String remoteRepoUri) {
		super(Settings.Type.GIT);
		this.remoteRepoUri = remoteRepoUri;
	}

	public enum Credentials {
		NONE,
		HTTPS,
		SSH
	}
}
