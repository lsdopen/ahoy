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
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.releases.ReleaseVersionRepository;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

@Service
@Slf4j
public class ReleaseService {
	private final EnvironmentRepository environmentRepository;
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ReleaseVersionRepository releaseVersionRepository;
	private final ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository;
	private final ApplicationEnvironmentConfigProvider environmentConfigProvider;
	private final ApplicationReleaseStatusRepository applicationReleaseStatusRepository;
	private final ReleaseManager releaseManager;
	private ApplicationEventPublisher eventPublisher;

	public ReleaseService(EnvironmentRepository environmentRepository,
	                      EnvironmentReleaseRepository environmentReleaseRepository,
	                      ReleaseVersionRepository releaseVersionRepository,
	                      ApplicationEnvironmentConfigRepository applicationEnvironmentConfigRepository,
	                      ApplicationEnvironmentConfigProvider environmentConfigProvider,
	                      ApplicationReleaseStatusRepository applicationReleaseStatusRepository,
	                      ReleaseManager releaseManager) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.releaseVersionRepository = releaseVersionRepository;
		this.applicationEnvironmentConfigRepository = applicationEnvironmentConfigRepository;
		this.environmentConfigProvider = environmentConfigProvider;
		this.applicationReleaseStatusRepository = applicationReleaseStatusRepository;
		this.releaseManager = releaseManager;
	}

	@Autowired
	public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Async("deploymentTaskExecutor")
	@Transactional
	public Future<EnvironmentRelease> deploy(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, DeployDetails deployDetails) {
		log.info("Deploying environment release: {}, release version: {}", environmentRelease, releaseVersion);

		ReleaseVersion previousReleaseVersion = environmentRelease.getCurrentReleaseVersion();
		boolean redeploy = releaseVersion.equals(previousReleaseVersion);
		boolean upgrade = previousReleaseVersion != null && !redeploy;

		ArgoApplication argoApplication = releaseManager.deploy(environmentRelease, releaseVersion, deployDetails);

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
	public Future<EnvironmentRelease> undeploy(EnvironmentRelease environmentRelease) {
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
				undeploy(environmentRelease).get();
			} catch (Exception e) {
				throw new EnvironmentException("Failed to undeploy " + environmentRelease + " from " + environmentRelease.getEnvironment(), e);
			}
		}

		environmentReleaseRepository.delete(environmentRelease);

		return new AsyncResult<>(environmentRelease);
	}

	@Transactional
	public EnvironmentRelease promote(Long environmentId, Long releaseId, Long destEnvironmentId) {
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(environmentId, releaseId);
		log.info("Promoting environment release: {} to environment: {}", environmentReleaseId, destEnvironmentId);

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		Optional<EnvironmentRelease> optionalPromotedEnvironmentRelease = environmentReleaseRepository.findById(new EnvironmentReleaseId(destEnvironmentId, releaseId));

		if (optionalPromotedEnvironmentRelease.isPresent()) {
			return optionalPromotedEnvironmentRelease.get();

		} else {
			Environment destEnvironment = environmentRepository.findById(destEnvironmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Could not find destination environment: " + destEnvironmentId));

			EnvironmentRelease promotedEnvironmentRelease = new EnvironmentRelease();
			promotedEnvironmentRelease.setId(new EnvironmentReleaseId());
			promotedEnvironmentRelease.setRelease(environmentRelease.getRelease());
			promotedEnvironmentRelease.setEnvironment(destEnvironment);

			return environmentReleaseRepository.save(promotedEnvironmentRelease);
		}
	}

	@Transactional
	public ReleaseVersion upgrade(Long releaseVersionId, String version) {
		ReleaseVersion currentReleaseVersion = releaseVersionRepository.findById(releaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find release version: " + releaseVersionId));

		log.info("Upgrading release version: {} to version: {}", currentReleaseVersion.getVersion(), version);

		ReleaseVersion upgradedReleaseVersion = new ReleaseVersion(version, currentReleaseVersion.getRelease(), new ArrayList<>(currentReleaseVersion.getApplicationVersions()));
		upgradedReleaseVersion = releaseVersionRepository.save(upgradedReleaseVersion);

		Iterable<EnvironmentRelease> environmentReleases = environmentReleaseRepository.findByRelease_Id_OrderByEnvironmentId(currentReleaseVersion.getRelease().getId());
		for (EnvironmentRelease environmentRelease : environmentReleases) {

			for (ApplicationVersion applicationVersion : upgradedReleaseVersion.getApplicationVersions()) {
				Optional<ApplicationEnvironmentConfig> currentEnvironmentConfig = environmentConfigProvider.environmentConfigFor(
					environmentRelease, currentReleaseVersion, applicationVersion);

				if (currentEnvironmentConfig.isPresent()) {
					ApplicationDeploymentId id = new ApplicationDeploymentId(
						environmentRelease.getId(),
						upgradedReleaseVersion.getId(),
						applicationVersion.getId());

					ApplicationEnvironmentConfig newEnvironmentConfig = new ApplicationEnvironmentConfig(id, currentEnvironmentConfig.get());
					applicationEnvironmentConfigRepository.save(newEnvironmentConfig);
				}
			}
		}

		return upgradedReleaseVersion;
	}

	@Transactional
	public EnvironmentRelease copyEnvConfig(Long environmentId, Long releaseId, Long sourceReleaseVersionId, Long destReleaseVersionId) {
		EnvironmentReleaseId environmentReleaseId = new EnvironmentReleaseId(environmentId, releaseId);
		log.info("Copying environment config for release: {} from sourceReleaseVersionId: {} to destReleaseVersionId: {}", environmentReleaseId, sourceReleaseVersionId, destReleaseVersionId);

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));
		ReleaseVersion sourceReleaseVersion = releaseVersionRepository.findById(sourceReleaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find sourceReleaseVersionId: " + sourceReleaseVersionId));
		ReleaseVersion destReleaseVersion = releaseVersionRepository.findById(destReleaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destReleaseVersionId: " + destReleaseVersionId));

		for (ApplicationVersion applicationVersion : destReleaseVersion.getApplicationVersions()) {
			Optional<ApplicationEnvironmentConfig> sourceConfig = environmentConfigProvider.environmentConfigFor(
				environmentRelease, sourceReleaseVersion, applicationVersion);

			if (sourceConfig.isPresent()) {
				Optional<ApplicationEnvironmentConfig> destConfig = environmentConfigProvider.environmentConfigFor(
					environmentRelease, destReleaseVersion, applicationVersion);

				if (destConfig.isEmpty()) {
					ApplicationDeploymentId id = new ApplicationDeploymentId(
						environmentRelease.getId(),
						destReleaseVersion.getId(),
						applicationVersion.getId());
					applicationEnvironmentConfigRepository.save(new ApplicationEnvironmentConfig(id, sourceConfig.get()));
				}
			}
		}

		return environmentRelease;
	}

	@Async("deploymentTaskExecutor")
	@Transactional
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
