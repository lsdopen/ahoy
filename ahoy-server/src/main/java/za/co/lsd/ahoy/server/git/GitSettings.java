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

package za.co.lsd.ahoy.server.git;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitSettings {
	@Id
	private Long id;
	private String remoteRepoUri;
	private String branch = "master";
	@Enumerated(EnumType.STRING)
	private Credentials credentials;
	@ToString.Exclude
	private String httpsUsername;
	@ToString.Exclude
	private String httpsPassword;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String privateKey;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String sshKnownHosts;

	public GitSettings(String remoteRepoUri) {
		this.remoteRepoUri = remoteRepoUri;
	}

	public GitSettings(GitSettingsDTO dto) {
		this.id = dto.getId();
		this.remoteRepoUri = dto.getRemoteRepoUri();
		this.branch = dto.getBranch();
		this.credentials = dto.getCredentials();
		this.httpsUsername = dto.getHttpsUsername();
		this.httpsPassword = dto.getHttpsPassword();
		this.privateKey = dto.getPrivateKey();
		this.sshKnownHosts = dto.getSshKnownHosts();
	}

	public enum Credentials {
		NONE,
		HTTPS,
		SSH
	}
}
