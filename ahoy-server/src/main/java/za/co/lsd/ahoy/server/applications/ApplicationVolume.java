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
public class ApplicationVolume {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name;
	@NotNull
	private String mountPath;
	@NotNull
	@Enumerated(EnumType.STRING)
	private VolumeType type;

	// PersistentVolume
	private String storageClassName;
	@Enumerated(EnumType.STRING)
	private VolumeAccessMode accessMode;
	private Long size;
	@Enumerated(EnumType.STRING)
	private StorageUnit sizeStorageUnit;

	// Secret
	private String secretName;

	@ManyToOne
	@JsonBackReference
	@ToString.Exclude
	private ApplicationVersion applicationVersion;

	public ApplicationVolume(String name, String mountPath, String storageClassName, VolumeAccessMode accessMode, Long size, StorageUnit sizeStorageUnit) {
		this.name = name;
		this.mountPath = mountPath;
		this.type = VolumeType.PersistentVolume;
		this.storageClassName = storageClassName;
		this.accessMode = accessMode;
		this.size = size;
		this.sizeStorageUnit = sizeStorageUnit;
	}

	public ApplicationVolume(String name, String mountPath, String secretName) {
		this.name = name;
		this.mountPath = mountPath;
		this.type = VolumeType.Secret;
		this.secretName = secretName;
	}
}
