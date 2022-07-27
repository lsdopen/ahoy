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

package za.co.lsd.ahoy.server.helm.values;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationVolume;
import za.co.lsd.ahoy.server.applications.VolumeType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationVolumeValues {
	private String name;
	private String mountPath;
	private String type;
	private String storageClassName;
	private String accessMode;
	private String size;
	private String secretName;

	public ApplicationVolumeValues(ApplicationVolume volume) {
		this.name = volume.getName();
		this.mountPath = volume.getMountPath();
		this.type = volume.getType().name();
		if (volume.getType().equals(VolumeType.PersistentVolume)) {
			this.storageClassName = volume.getStorageClassName();
			this.accessMode = volume.getAccessMode().name();
			this.size = volume.getSize() == null ? null : volume.getSize() + volume.getSizeStorageUnit().name();

		} else if (volume.getType().equals(VolumeType.Secret)) {
			this.secretName = volume.getSecretName();

		} else if (volume.getType().equals(VolumeType.EmptyDir)) {
			this.size = volume.getSize() == null ? null : volume.getSize() + volume.getSizeStorageUnit().name();
		}
	}

	public static ApplicationVolumeValues createPersistentVolumeValues(String name, String mountPath, String storageClassName, String accessMode, String size) {
		return new ApplicationVolumeValues(name, mountPath, VolumeType.PersistentVolume.name(), storageClassName, accessMode, size, null);
	}

	public static ApplicationVolumeValues createSecretVolumeValues(String name, String mountPath, String secretName) {
		return new ApplicationVolumeValues(name, mountPath, VolumeType.Secret.name(), null, null, null, secretName);
	}

	public static ApplicationVolumeValues createEmptyDirVolumeValues(String name, String mountPath, String size) {
		return new ApplicationVolumeValues(name, mountPath, VolumeType.EmptyDir.name(), null, null, size, null);
	}
}
