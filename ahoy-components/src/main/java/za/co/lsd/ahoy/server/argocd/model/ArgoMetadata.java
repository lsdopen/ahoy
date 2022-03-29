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

package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoMetadata {
	public static final String MANAGED_BY_LABEL = "managedBy";
	public static final String CLUSTER_NAME_LABEL = "clusterName";
	public static final String ENVIRONMENT_NAME_LABEL = "environmentName";
	public static final String RELEASE_NAME_LABEL = "releaseName";
	public static final String RELEASE_VERSION_LABEL = "releaseVersion";

	private String name;
	private String uid;
	private Map<String, String> labels;
}
