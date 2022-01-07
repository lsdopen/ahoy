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

package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.lsd.ahoy.server.applications.*;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoMetadata;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.argocd.model.ResourceStatus;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentException;
import za.co.lsd.ahoy.server.environments.EnvironmentRepository;
import za.co.lsd.ahoy.server.releases.*;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.RunAsRole;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
	private ApplicationEventPublisher eventPublisher;

	public ReleaseService(EnvironmentRepository environmentRepository,
						  EnvironmentReleaseRepository environmentReleaseRepository,
						  ReleaseRepository releaseRepository,
						  ReleaseVersionRepository releaseVersionRepository,
						  ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository,
						  ApplicationEnvironmentConfigProvider environmentConfigProvider,
						  ApplicationReleaseStatusRepository applicationReleaseStatusRepository,
						  ApplicationVersionRepository applicationVersionRepository,
						  ReleaseManager releaseManager) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.releaseRepository = releaseRepository;
		this.releaseVersionRepository = releaseVersionRepository;
		this.applicationEnvironmentConfigRepository = applicationEnvironmentConfigRepository;
		this.environmentConfigProvider = environmentConfigProvider;
		this.applicationReleaseStatusRepository = applicationReleaseStatusRepository;
		this.applicationVersionRepository = applicationVersionRepository;
		this.releaseManager = releaseManager;
	}

	@Autowired
	public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Async("deploymentTaskExecutor")
	@Transactional
	public Future<EnvironmentRelease> deploy(EnvironmentReleaseId environmentReleaseId, DeployOptions deployOptions) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		ReleaseVersion releaseVersion = releaseVersionRepository.findById(deployOptions.getReleaseVersionId())
			.orElseThrow(() -> new ResourceNotFoundException("Could not find releaseVersion in release, releaseVersionId: " + deployOptions.getReleaseVersionId()));

		log.info("Deploying environment release: {}, release version: {}", environmentRelease, releaseVersion);

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

		return new AsyncResult<>(environmentRelease);
	}

	@Async("deploymentTaskExecutor")
	@Transactional
	public Future<EnvironmentRelease> undeploy(EnvironmentReleaseId environmentReleaseId) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		log.info("Undeploying environment release: {}", environmentRelease);

		ReleaseVersion currentReleaseVersion = environmentRelease.getCurrentReleaseVersion();

		releaseManager.undeploy(environmentRelease);

		environmentRelease.setCurrentReleaseVersion(null);
		environmentRelease.setApplicationsReady(null);
		environmentRelease.setStatus(null);
		environmentRelease.setArgoCdName(null);
		environmentRelease.setArgoCdUid(null);

		environmentRelease.setPreviousReleaseVersion(null);

		purgeReleaseStatus(environmentRelease, currentReleaseVersion);

		environmentReleaseRepository.save(environmentRelease);
		log.info("Undeployed environment release: {}", environmentRelease);

		return new AsyncResult<>(environmentRelease);
	}

	@Transactional
	public Future<EnvironmentRelease> remove(EnvironmentReleaseId environmentReleaseId) {
		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		log.info("Removing environment release: {}", environmentRelease);

		if (environmentRelease.hasCurrentReleaseVersion()) {
			log.info("{} is currently deployed in {}, undeploying...", environmentRelease.getRelease().getName(), environmentRelease.getEnvironment().getName());
			try {
				undeploy(environmentReleaseId).get();
			} catch (Exception e) {
				throw new EnvironmentException("Failed to undeploy " + environmentRelease + " from " + environmentRelease.getEnvironment(), e);
			}
		}

		environmentReleaseRepository.delete(environmentRelease);

		return new AsyncResult<>(environmentRelease);
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

		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion(upgradeOptions.getVersion(), currentReleaseVersion.getRelease(), new ArrayList<>(currentReleaseVersion.getApplicationVersions()));
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
	public Release duplicate(Long releaseId, DuplicateOptions duplicateOptions) {
		Release sourceRelease = releaseRepository.findById(releaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find source release: " + releaseId));

		Release duplicatedRelease = new Release();
		duplicatedRelease.setName(duplicateOptions.getReleaseName());
		duplicatedRelease.setReleaseVersions(new ArrayList<>());
		duplicatedRelease.setEnvironmentReleases(new ArrayList<>());
		duplicatedRelease = releaseRepository.save(duplicatedRelease);
		log.debug("Duplicated release: {} for source release: {}", duplicatedRelease, sourceRelease);

		for (ReleaseVersion sourceReleaseVersion : sourceRelease.getReleaseVersions()) {
			ReleaseVersion duplicatedReleaseVersion = new ReleaseVersion();
			duplicatedReleaseVersion.setRelease(duplicatedRelease);
			duplicatedReleaseVersion.setVersion(sourceReleaseVersion.getVersion());
			duplicatedReleaseVersion.setApplicationVersions(sourceReleaseVersion.getApplicationVersions()
				.stream().collect(Collectors.toList()));
			duplicatedReleaseVersion = releaseVersionRepository.save(duplicatedReleaseVersion);
			log.debug("Duplicated release version: {} for source release version: {}", duplicatedReleaseVersion, sourceReleaseVersion);

			duplicatedRelease.getReleaseVersions().add(duplicatedReleaseVersion);
		}

		if (duplicateOptions.isAddToSameEnvironments()) {
			for (EnvironmentRelease sourceEnvRelease : sourceRelease.getEnvironmentReleases()) {
				EnvironmentRelease duplicatedEnvRelease = new EnvironmentRelease();
				duplicatedEnvRelease.setId(new EnvironmentReleaseId());
				duplicatedEnvRelease.setRelease(duplicatedRelease);
				duplicatedEnvRelease.setEnvironment(sourceEnvRelease.getEnvironment());
				duplicatedEnvRelease = environmentReleaseRepository.save(duplicatedEnvRelease);
				log.debug("Duplicated environment release: {} for source environment release: {}", duplicatedEnvRelease.getId(), sourceEnvRelease.getId());

				duplicatedRelease.getEnvironmentReleases().add(duplicatedEnvRelease);

				if (duplicateOptions.isCopyEnvironmentConfig()) {
					for (ReleaseVersion destReleaseVersion : duplicatedRelease.getReleaseVersions()) {
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

		return duplicatedRelease;
	}

	@Transactional
	public EnvironmentRelease copyEnvConfig(EnvironmentReleaseId environmentReleaseId, Long sourceReleaseVersionId, Long destReleaseVersionId) {
		log.info("Copying environment config for release: {} from sourceReleaseVersionId: {} to destReleaseVersionId: {}", environmentReleaseId, sourceReleaseVersionId, destReleaseVersionId);

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
		log.info("Copying environment config for release version: {} from source application version: {} to dest application version: {}", releaseVersionId, sourceApplicationVersionId, destApplicationVersionId);

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

	/**
	 * Copies environment config from one release version to another for the same environment release.
	 */
	private void copyEnvironmentConfig(EnvironmentRelease environmentRelease, ReleaseVersion sourceReleaseVersion, ReleaseVersion destReleaseVersion) {
		this.copyEnvironmentConfig(environmentRelease, sourceReleaseVersion, environmentRelease, destReleaseVersion);
	}

	/**
	 * Copies environment config from one environment release to another for the same version.
	 */
	private void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, EnvironmentRelease destEnvironmentRelease, ReleaseVersion releaseVersion) {
		this.copyEnvironmentConfig(sourceEnvironmentRelease, releaseVersion, destEnvironmentRelease, releaseVersion);
	}

	/**
	 * Copies environment config from one environment release version to another environment release version.
	 */
	private void copyEnvironmentConfig(EnvironmentRelease sourceEnvironmentRelease, ReleaseVersion sourceReleaseVersion, EnvironmentRelease destEnvironmentRelease, ReleaseVersion destReleaseVersion) {
		for (ApplicationVersion applicationVersion : destReleaseVersion.getApplicationVersions()) {
			copyEnvironmentConfig(sourceEnvironmentRelease, sourceReleaseVersion, applicationVersion, destEnvironmentRelease, destReleaseVersion, applicationVersion);
		}
	}

	/**
	 * Copies environment config from one application version to another for the same release version and environment.
	 */
	private void copyEnvironmentConfig(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, ApplicationVersion sourceApplicationVersion, ApplicationVersion destApplicationVersion) {
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
				log.info("Saving new application env config for id {}", id);
				applicationEnvironmentConfigRepository.save(new ApplicationEnvironmentConfig(id, sourceConfig.get()));
			}
		}
	}

	@Async("deploymentTaskExecutor")
	@Transactional
	@RunAsRole(Role.admin)
	public void updateStatus(ArgoApplication application) {
		Objects.requireNonNull(application, "application is required");

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

	private boolean updateStatus(ArgoApplication application, EnvironmentRelease environmentRelease) {
		boolean environmentReleaseChanged = false;
		if (environmentRelease.hasCurrentReleaseVersion()) {
			ReleaseVersion currentReleaseVersion = environmentRelease.getCurrentReleaseVersion();

			String environmentName = environmentRelease.getEnvironment().getName();
			String releaseName = environmentRelease.getRelease().getName();
			ArgoApplication.Status argoApplicationStatus = application.getStatus();
			int applicationsHealthy = 0;
			for (ApplicationVersion applicationVersion : currentReleaseVersion.getApplicationVersions()) {
				String applicationName = releaseName + "-" + applicationVersion.getApplication().getName();

				ApplicationDeploymentId id = new ApplicationDeploymentId(environmentRelease.getId(), currentReleaseVersion.getId(), applicationVersion.getId());
				ApplicationReleaseStatus status = applicationReleaseStatusRepository.findById(id)
					.orElse(new ApplicationReleaseStatus(id));

				int statusHashCode = status.hashCode();
				Optional<ResourceStatus> deploymentStatusOptional = argoApplicationStatus.getDeploymentResource(applicationName);
				if (deploymentStatusOptional.isPresent()) {
					ResourceStatus deploymentStatus = deploymentStatusOptional.get();
					status.setStatus(deploymentStatus.getHealth().getStatus());

				} else {
					status.setStatus(HealthStatus.StatusCode.Unknown);
				}

				boolean changed = status.hashCode() != statusHashCode;
				if (changed) {
					applicationReleaseStatusRepository.save(status);
					log.info("Status changed for application {} in release {} in environment {}: {}", applicationName, releaseName, environmentName, status);
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
