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

package za.co.lsd.ahoy.server.applications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationVolume {
	@NotNull
	private String name;
	@NotNull
	private String mountPath;
	@NotNull
	private VolumeType type;

	// PersistentVolume
	private String storageClassName;
	private VolumeAccessMode accessMode;
	private Long size;
	private StorageUnit sizeStorageUnit;

	// Secret
	private String secretName;

	public static ApplicationVolume createPersistentVolume(String name, String mountPath, String storageClassName, VolumeAccessMode accessMode, Long size, StorageUnit sizeStorageUnit) {
		return new ApplicationVolume(name, mountPath, VolumeType.PersistentVolume, storageClassName, accessMode, size, sizeStorageUnit, null);
	}

	public static ApplicationVolume createSecretVolume(String name, String mountPath, String secretName) {
		return new ApplicationVolume(name, mountPath, VolumeType.Secret, null, null, null, null, secretName);
	}

	public static ApplicationVolume createEmptyDirVolume(String name, String mountPath, Long size, StorageUnit sizeStorageUnit) {
		return new ApplicationVolume(name, mountPath, VolumeType.EmptyDir, null, null, size, sizeStorageUnit, null);
	}
}
