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
