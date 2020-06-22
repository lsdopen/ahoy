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

package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoApplication {
	private ArgoMetadata metadata;
	private Spec spec;
	private Status status;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Spec {
		private String project;
		private Source source;
		private Destination destination;
		private ArgoSyncPolicy syncPolicy;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Source {
		private String repoURL;
		private String path;
		private String targetRevision;
		private Helm helm;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Destination {
		private String server;
		private String namespace;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Helm {
		private List<String> valueFiles;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Status {
		private HealthStatus health;
		private List<ResourceStatus> resources;

		public boolean hasResources() {
			return resources != null && resources.size() > 0;
		}

		public Optional<ResourceStatus> getDeploymentResource(String name) {
			if (hasResources()) {
				return resources.stream()
					.filter(resourceStatus -> resourceStatus.getKind().equals("Deployment") && resourceStatus.getName().equals(name))
					.findFirst();
			}
			return Optional.empty();
		}
	}
}
