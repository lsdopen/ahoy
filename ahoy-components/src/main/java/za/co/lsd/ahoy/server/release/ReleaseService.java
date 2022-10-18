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

package za.co.lsd.ahoy.server.release;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.argocd.model.*;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentException;
import za.co.lsd.ahoy.server.environments.EnvironmentRepository;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseRepository;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.releases.ReleaseVersionRepository;
import za.co.lsd.ahoy.server.releases.resources.ResourceNode;
import za.co.lsd.ahoy.server.releases.resources.ResourceTreeConverter;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.RunAsRole;
import za.co.lsd.ahoy.server.task.TaskProgressService;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class ReleaseService {
	private final EnvironmentRepository environmentRepository;
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ReleaseRepository releaseRepository;
	private final ReleaseVersionRepository releaseVersionRepository;
	private final ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository;
	private final ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private final ApplicationReleaseStatusRepository applicationReleaseStatusRepository;
	private final ApplicationVersionRepository applicationVersionRepository;
	private final ReleaseManager releaseManager;
	private final ResourceTreeConverter resourceTreeConverter;
	private ApplicationEventPublisher eventPublisher;
	private TaskProgressService taskProgressService;

	public ReleaseService(EnvironmentRepository environmentRepository,
						  EnvironmentReleaseRepository environmentReleaseRepository,
						  ReleaseRepository releaseRepository,
						  ReleaseVersionRepository releaseVersionRepository,
						  ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository,
						  ApplicationEnvironmentConfigProvider environmentConfigProvider,
						  ApplicationReleaseStatusRepository applicationReleaseStatusRepository,
						  ApplicationVersionRepository applicationVersionRepository,
						  ReleaseManager releaseManager, ResourceTreeConverter resourceTreeConverter) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.releaseRepository = releaseRepository;
		this.releaseVersionRepository = releaseVersionRepository;
		this.applicationEnvironmentConfigRepository = applicationEnvironmentConfigRepository;
		this.environmentConfigProvider = environmentConfigProvider;
		this.applicationReleaseStatusRepository = applicationReleaseStatusRepository;
		this.applicationVersionRepository = applicationVersionRepository;
		this.releaseManager = releaseManager;
		this.resourceTreeConverter = resourceTreeConverter;
	}

	@Autowired
	public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Autowired
	public void setTaskProgressService(TaskProgressService taskProgressService) {
		this.taskProgressService = taskProgressService;
	}

	@Transactional
	public EnvironmentRelease deploy(EnvironmentReleaseId environmentReleaseId, DeployOptions deployOptions) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		ReleaseVersion releaseVersion = releaseVersionRepository.findById(deployOptions.getReleaseVersionId())
			.orElseThrow(() -> new ResourceNotFoundException("Could not find releaseVersion in release, releaseVersionId: " + deployOptions.getReleaseVersionId()));

		log.info("Deploying environment release: {}, release version: {}", environmentRelease, releaseVersion);

		taskProgressService.progress("deploying");

		ReleaseVersion previousReleaseVersion = environmentRelease.getCurrentReleaseVersion();
		boolean redeploy = releaseVersion.equals(previousReleaseVersion);
		boolean upgrade = previousReleaseVersion != null && !redeploy;

		ArgoApplication argoApplication = releaseManager.deploy(environmentRelease, releaseVersion, deployOptions);

		environmentRelease.setCurrentReleaseVersion(releaseVersion);
		environmentRelease.setArgoCdName(argoApplication.getMetadata().getName());
		environmentRelease.setArgoCdUid(argoApplication.getMetadata().getUid());

		if (upgrade) {
			environmentRelease.setApplicationsReady(0);
			environmentRelease.setStatus(HealthStatus.StatusCode.Progressing);
			purgeReleaseStatus(environmentRelease, previousReleaseVersion);
			environmentRelease.setPreviousReleaseVersion(previousReleaseVersion);
		}

		environmentReleaseRepository.save(environmentRelease);
		log.info("Deployed environment release: {}", environmentRelease);

		return environmentRelease;
	}

	@Transactional
	public EnvironmentRelease undeploy(EnvironmentReleaseId environmentReleaseId) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		log.info("Undeploying environment release: {}", environmentRelease);

		ReleaseVersion currentReleaseVersion = environmentRelease.getCurrentReleaseVersion();
		taskProgressService.progress("undeploying");

		releaseManager.undeploy(environmentRelease);

		environmentRelease.setCurrentReleaseVersion(null);
		environmentRelease.setApplicationsReady(null);
		environmentRelease.setStatus(null);

		environmentRelease.setPreviousReleaseVersion(null);

		purgeReleaseStatus(environmentRelease, currentReleaseVersion);

		environmentReleaseRepository.save(environmentRelease);
		log.info("Undeployed environment release: {}", environmentRelease);

		return environmentRelease;
	}

	@Transactional
	public EnvironmentRelease remove(EnvironmentReleaseId environmentReleaseId) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		log.info("Removing environment release: {}", environmentRelease);

		if (environmentRelease.hasCurrentReleaseVersion()) {
			log.info("{} is currently deployed in {}, undeploying...", environmentRelease.getRelease().getName(), environmentRelease.getEnvironment().getName());
			try {
				undeploy(environmentReleaseId);
			} catch (Exception e) {
				throw new EnvironmentException("Failed to undeploy " + environmentRelease + " from " + environmentRelease.getEnvironment(), e);
			}
		}

		environmentReleaseRepository.delete(environmentRelease);

		return environmentRelease;
	}

	@Transactional
	public EnvironmentRelease promote(EnvironmentReleaseId environmentReleaseId, PromoteOptions promoteOptions) {
		Long destEnvironmentId = promoteOptions.getDestEnvironmentId();
		log.info("Promoting environment release: {} to environment: {}", environmentReleaseId, destEnvironmentId);

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		Optional<EnvironmentRelease> optionalPromotedEnvironmentRelease = environmentReleaseRepository.findById(new EnvironmentReleaseId(destEnvironmentId, environmentReleaseId.getReleaseId()));

		if (optionalPromotedEnvironmentRelease.isPresent()) {
			return optionalPromotedEnvironmentRelease.get();

		} else {
			Environment destEnvironment = environmentRepository.findById(destEnvironmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Could not find destination environment: " + destEnvironmentId));

			EnvironmentRelease promotedEnvironmentRelease = new EnvironmentRelease();
			promotedEnvironmentRelease.setId(new EnvironmentReleaseId());
			promotedEnvironmentRelease.setRelease(environmentRelease.getRelease());
			promotedEnvironmentRelease.setEnvironment(destEnvironment);
			promotedEnvironmentRelease = environmentReleaseRepository.save(promotedEnvironmentRelease);

			if (promoteOptions.isCopyEnvironmentConfig()) {
				log.info("Copy environment config selected, copying config to promoted environment release: {}", promotedEnvironmentRelease);

				for (ReleaseVersion releaseVersion : environmentRelease.getRelease().getReleaseVersions()) {
					copyEnvironmentConfig(environmentRelease, promotedEnvironmentRelease, releaseVersion);
				}
			}

			return promotedEnvironmentRelease;
		}
	}

	@Transactional
	public ReleaseVersion upgrade(Long releaseVersionId, UpgradeOptions upgradeOptions) {
		ReleaseVersion currentReleaseVersion = releaseVersionRepository.findById(releaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find release version: " + releaseVersionId));

		log.info("Upgrading release version: {} to version: {}", currentReleaseVersion, upgradeOptions.getVersion());

		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion(upgradeOptions.getVersion());
		upgradedReleaseVersion.setApplicationVersions(new ArrayList<>(currentReleaseVersion.getApplicationVersions()));
		currentReleaseVersion.getRelease().addReleaseVersion(upgradedReleaseVersion);
		upgradedReleaseVersion = releaseVersionRepository.save(upgradedReleaseVersion);

		if (upgradeOptions.isCopyEnvironmentConfig()) {
			log.info("Copy environment config selected, copying config to new version: {}", upgradedReleaseVersion);

			Iterable<EnvironmentRelease> environmentReleases = environmentReleaseRepository.findByRelease(currentReleaseVersion.getRelease().getId());
			for (EnvironmentRelease environmentRelease : environmentReleases) {
				copyEnvironmentConfig(environmentRelease, currentReleaseVersion, upgradedReleaseVersion);
			}
		}

		return upgradedReleaseVersion;
	}

	@Transactional
	public Release duplicate(Long sourceReleaseId, Long destReleaseId, DuplicateOptions duplicateOptions) {
		Release sourceRelease = releaseRepository.findById(sourceReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find source release: " + sourceReleaseId));

		Release destRelease = releaseRepository.findById(destReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destination release: " + sourceReleaseId));
		log.debug("Duplicating release: {} to release: {} with options: {}", sourceRelease, destRelease, duplicateOptions);

		for (ReleaseVersion sourceReleaseVersion : sourceRelease.getReleaseVersions()) {
			ReleaseVersion duplicatedReleaseVersion = new ReleaseVersion(sourceReleaseVersion.getVersion());
			destRelease.addReleaseVersion(duplicatedReleaseVersion);
			duplicatedReleaseVersion.setApplicationVersions(new ArrayList<>(sourceReleaseVersion.getApplicationVersions()));
			duplicatedReleaseVersion = releaseVersionRepository.save(duplicatedReleaseVersion);
			log.debug("Duplicated release version: {} for source release version: {}", duplicatedReleaseVersion, sourceReleaseVersion);
		}

		if (duplicateOptions.isAddToSameEnvironments()) {
			for (EnvironmentRelease sourceEnvRelease : sourceRelease.getEnvironmentReleases()) {
				EnvironmentRelease duplicatedEnvRelease = new EnvironmentRelease(sourceEnvRelease.getEnvironment(), destRelease);
				duplicatedEnvRelease = environmentReleaseRepository.save(duplicatedEnvRelease);
				log.debug("Duplicated environment release: {} for source environment release: {}", duplicatedEnvRelease.getId(), sourceEnvRelease.getId());

				if (duplicateOptions.isCopyEnvironmentConfig()) {
					for (ReleaseVersion destReleaseVersion : destRelease.getReleaseVersions()) {
						ReleaseVersion sourceReleaseVersion = sourceRelease.getReleaseVersions().stream()
							.filter(rv -> rv.getVersion().equals(destReleaseVersion.getVersion()))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("Should have been able to find matching release versions"));

						log.debug("Copying env config from: sourceEnvRelease {}, sourceReleaseVersion {} to: duplicatedEnvRelease {}, destReleaseVersion {}",
							sourceEnvRelease.getId(), sourceReleaseVersion.getId(), duplicatedEnvRelease.getId(), destReleaseVersion.getId());

						copyEnvironmentConfig(sourceEnvRelease, sourceReleaseVersion, duplicatedEnvRelease, destReleaseVersion);
					}
				}
			}
		}

		return destRelease;
	}

	@Transactional
	public EnvironmentRelease copyEnvConfig(EnvironmentReleaseId environmentReleaseId, Long sourceReleaseVersionId, Long destReleaseVersionId) {
		log.debug("Copying environment config for release: {} from sourceReleaseVersionId: {} to destReleaseVersionId: {}", environmentReleaseId, sourceReleaseVersionId, destReleaseVersionId);

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));
		ReleaseVersion sourceReleaseVersion = releaseVersionRepository.findById(sourceReleaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find sourceReleaseVersionId: " + sourceReleaseVersionId));
		ReleaseVersion destReleaseVersion = releaseVersionRepository.findById(destReleaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destReleaseVersionId: " + destReleaseVersionId));

		copyEnvironmentConfig(environmentRelease, sourceReleaseVersion, destReleaseVersion);

		return environmentRelease;
	}

	/**
	 * Copies environment config from one application version to another for the same release version across all its environments that the release belongs to.
	 * <p>
	 * Does not copy if the destination application version environment config already exists.
	 *
	 * @param releaseVersionId           the release version id to copy for
	 * @param sourceApplicationVersionId the source application version id to copy from
	 * @param destApplicationVersionId   the destination application version id to copy to
	 */
	@Transactional
	public void copyApplicationVersionEnvConfig(Long releaseVersionId, Long sourceApplicationVersionId, Long destApplicationVersionId) {
		log.debug("Copying environment config for release version: {} from source application version: {} to dest application version: {}", releaseVersionId, sourceApplicationVersionId, destApplicationVersionId);

		ReleaseVersion releaseVersion = releaseVersionRepository.findById(releaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find releaseVersion: " + releaseVersionId));
		ApplicationVersion sourceApplicationVersion = applicationVersionRepository.findById(sourceApplicationVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find sourceApplicationVersion: " + sourceApplicationVersionId));
		ApplicationVersion destApplicationVersion = applicationVersionRepository.findById(destApplicationVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destApplicationVersion: " + destApplicationVersionId));

		Iterable<EnvironmentRelease> environmentReleases = environmentReleaseRepository.findByRelease(releaseVersion.getRelease().getId());
		for (EnvironmentRelease environmentRelease : environmentReleases) {
			copyEnvironmentConfig(environmentRelease, releaseVersion, sourceApplicationVersion, destApplicationVersion);
		}
	}

	public Optional<ResourceNode> getResources(EnvironmentReleaseId environmentReleaseId) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		Optional<ResourceTree> resourceTree = releaseManager.getResourceTree(environmentRelease);
		return resourceTree.map(resourceTreeConverter::convert);
	}

	public Optional<Resource> getResource(EnvironmentReleaseId environmentReleaseId, String resourceNamespace, String resourceName, String version, String kind) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		return releaseManager.getResource(environmentRelease, resourceNamespace, resourceName, version, kind);
	}

	public Optional<ArgoEvents> getEvents(EnvironmentReleaseId environmentReleaseId, String resourceUid, String resourceNamespace, String resourceName) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		return releaseManager.getEvents(environmentRelease, resourceUid, resourceNamespace, resourceName);
	}

	public Flux<PodLog> getLogs(EnvironmentReleaseId environmentReleaseId,
								String podName,
								String resourceNamespace,
								String container) {
		log.debug("Getting logs for environment release: {}, podName: {}", environmentReleaseId, podName);

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		return releaseManager.getLogs(environmentRelease, podName, resourceNamespace, container);
	}

	/**
	 * Copies environment config from one release version to another for the same environment release.
	 */
	public void copyEnvironmentConfig(EnvironmentRelease environmentRelease, ReleaseVersion sourceReleaseVersion, ReleaseVersion destReleaseVersion) {
		copyEnvironmentConfig(environmentRelease, sourceReleaseVersion, environmentRelease, destReleaseVersion);
	}

	/**
	 * Copies environment config from one environment release to another for all its release versions. Usually used when duplicating and environment with all its releases.
	 */
	public void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, EnvironmentRelease destEnvironmentRelease) {
		for (ReleaseVersion releaseVersion : sourceEnvironmentRelease.getRelease().getReleaseVersions()) {
			copyEnvironmentConfig(sourceEnvironmentRelease, destEnvironmentRelease, releaseVersion);
		}
	}

	/**
	 * Copies environment config from one environment release to another for the same version.
	 */
	public void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, EnvironmentRelease destEnvironmentRelease, ReleaseVersion releaseVersion) {
		copyEnvironmentConfig(sourceEnvironmentRelease, releaseVersion, destEnvironmentRelease, releaseVersion);
	}

	/**
	 * Copies environment config from one environment release version to another environment release version.
	 */
	public void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, ReleaseVersion sourceReleaseVersion, EnvironmentRelease destEnvironmentRelease, ReleaseVersion destReleaseVersion) {
		for (ApplicationVersion applicationVersion : destReleaseVersion.getApplicationVersions()) {
			copyEnvironmentConfig(sourceEnvironmentRelease, sourceReleaseVersion, applicationVersion, destEnvironmentRelease, destReleaseVersion, applicationVersion);
		}
	}

	/**
	 * Copies environment config from one application version to another for the same release version and environment.
	 */
	public void copyEnvironmentConfig(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, ApplicationVersion sourceApplicationVersion, ApplicationVersion destApplicationVersion) {
		copyEnvironmentConfig(environmentRelease, releaseVersion, sourceApplicationVersion, environmentRelease, releaseVersion, destApplicationVersion);
	}

	private void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, ReleaseVersion sourceReleaseVersion, ApplicationVersion sourceApplicationVersion,
									   EnvironmentRelease destEnvironmentRelease, ReleaseVersion destReleaseVersion, ApplicationVersion destApplicationVersion) {
		Optional<ApplicationEnvironmentConfig> sourceConfig = environmentConfigProvider.environmentConfigFor(
			sourceEnvironmentRelease, sourceReleaseVersion, sourceApplicationVersion);

		if (sourceConfig.isPresent()) {
			Optional<ApplicationEnvironmentConfig> destConfig = environmentConfigProvider.environmentConfigFor(
				destEnvironmentRelease, destReleaseVersion, destApplicationVersion);

			if (destConfig.isEmpty()) {
				ApplicationDeploymentId id = new ApplicationDeploymentId(
					destEnvironmentRelease.getId(),
					destReleaseVersion.getId(),
					destApplicationVersion.getId());
				log.debug("Saving new application env config for id {}", id);
				applicationEnvironmentConfigRepository.save(new ApplicationEnvironmentConfig(id, sourceConfig.get()));
			}
		}
	}

	@Transactional
	@RunAsRole(Role.admin)
	public void updateStatus(ArgoApplication application) {
		Objects.requireNonNull(application, "application is required");

		if (application.getStatus().hasResources()) {
			log.trace("Updating status for argo application {}", application);

			Optional<EnvironmentRelease> environmentReleaseOptional = environmentReleaseRepository.findByArgoCdUid(application.getMetadata().getUid());
			if (environmentReleaseOptional.isPresent()) {
				EnvironmentRelease environmentRelease = environmentReleaseOptional.get();

				if (environmentRelease.hasCurrentReleaseVersion() &&
					environmentRelease.getCurrentReleaseVersion().getVersion().equals(application.getMetadata().getLabels().get(ArgoMetadata.RELEASE_VERSION_LABEL))) {

					if (updateStatus(application, environmentRelease)) {
						eventPublisher.publishEvent(
							new ReleaseStatusChangedEvent(this,
								environmentRelease.getId(),
								environmentRelease.getCurrentReleaseVersion().getId()));
					}
				}
			}
		}
	}

	@RunAsRole(Role.admin)
	public void updateDeleted(ArgoApplication application) {
		Optional<EnvironmentRelease> environmentReleaseOptional = environmentReleaseRepository.findByArgoCdUid(application.getMetadata().getUid());
		if (environmentReleaseOptional.isPresent()) {
			EnvironmentRelease environmentRelease = environmentReleaseOptional.get();

			eventPublisher.publishEvent(
				new ReleaseStatusChangedEvent(this,
					environmentRelease.getId(),
					null));
		}
	}

	private boolean updateStatus(ArgoApplication application, EnvironmentRelease environmentRelease) {
		boolean environmentReleaseChanged = false;
		if (environmentRelease.hasCurrentReleaseVersion()) {
			ReleaseVersion currentReleaseVersion = environmentRelease.getCurrentReleaseVersion();

			String environmentName = environmentRelease.getEnvironment().getName();
			String releaseName = environmentRelease.getRelease().getName();
			ArgoApplication.Status argoApplicationStatus = application.getStatus();
			int applicationsHealthy = 0;
			for (ApplicationVersion applicationVersion : currentReleaseVersion.getApplicationVersions()) {
				String applicationName = applicationVersion.getApplication().getName();

				ApplicationDeploymentId id = new ApplicationDeploymentId(environmentRelease.getId(), currentReleaseVersion.getId(), applicationVersion.getId());
				ApplicationReleaseStatus status = applicationReleaseStatusRepository.findById(id)
					.orElse(new ApplicationReleaseStatus(id));

				int statusHash = status.hash();
				Optional<ResourceStatus> deploymentStatusOptional = argoApplicationStatus.getDeploymentResource(applicationName);
				if (deploymentStatusOptional.isPresent()) {
					ResourceStatus deploymentStatus = deploymentStatusOptional.get();
					status.setStatus(deploymentStatus.getHealth().getStatus());

				} else {
					status.setStatus(HealthStatus.StatusCode.Unknown);
				}

				boolean changed = status.hash() != statusHash;
				if (changed) {
					applicationReleaseStatusRepository.save(status);
					log.trace("Status changed for application {} in release {} in environment {}: {}", applicationName, releaseName, environmentName, status);
					environmentReleaseChanged = true;
				}

				if (HealthStatus.StatusCode.Healthy.equals(status.getStatus()))
					applicationsHealthy++;
			}

			environmentRelease.setApplicationsReady(applicationsHealthy);

			HealthStatus.StatusCode environmentReleaseStatus = argoApplicationStatus.getHealth().getStatus();
			if (!environmentReleaseStatus.equals(environmentRelease.getStatus())) {
				environmentRelease.setStatus(environmentReleaseStatus);
				environmentReleaseChanged = true;
			}
		}
		return environmentReleaseChanged;
	}

	private void purgeReleaseStatus(EnvironmentRelease environmentRelease, ReleaseVersion currentReleaseVersion) {
		Iterable<ApplicationReleaseStatus> applicationReleaseStatuses =
			applicationReleaseStatusRepository.byReleaseVersion(environmentRelease.getId().getEnvironmentId(), environmentRelease.getId().getReleaseId(), currentReleaseVersion.getId());

		applicationReleaseStatusRepository.deleteAll(applicationReleaseStatuses);
	}
}
