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

package za.co.lsd.ahoy.server.environments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.lsd.ahoy.server.cluster.Cluster;
import za.co.lsd.ahoy.server.cluster.ClusterRepository;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.release.DeployOptions;
import za.co.lsd.ahoy.server.release.ReleaseService;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RepositoryEventHandler
@Slf4j
public class EnvironmentService {
	private final EnvironmentRepository environmentRepository;
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ClusterRepository clusterRepository;
	private final ReleaseService releaseService;

	public EnvironmentService(EnvironmentRepository environmentRepository, EnvironmentReleaseRepository environmentReleaseRepository, ClusterRepository clusterRepository, ReleaseService releaseService) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.clusterRepository = clusterRepository;
		this.releaseService = releaseService;
	}

	@Transactional
	public Environment delete(Long environmentId) {
		Environment environment = environmentRepository.findById(environmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment: " + environmentId));

		return delete(environment);
	}

	@Transactional
	public Environment delete(Environment environment) {
		log.info("Deleting environment: {}", environment);

		undeployReleasesFrom(environment);

		environmentRepository.delete(environment);
		return environment;
	}

	@Transactional
	public Environment duplicate(Long sourceEnvironmentId, Long destEnvironmentId, DuplicateOptions duplicateOptions) {
		Environment sourceEnvironment = environmentRepository.findById(sourceEnvironmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find source environment: " + sourceEnvironmentId));

		Environment destEnvironment = environmentRepository.findById(destEnvironmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destination environment: " + destEnvironmentId));

		log.info("Duplicating environment {} to environment {} with options {}", sourceEnvironment, destEnvironment, duplicateOptions);

		List<EnvironmentRelease> sourceEnvironmentReleases = sourceEnvironment.getEnvironmentReleases();
		for (EnvironmentRelease sourceEnvironmentRelease : sourceEnvironmentReleases) {

			EnvironmentRelease newEnvironmentRelease = new EnvironmentRelease(destEnvironment, sourceEnvironmentRelease.getRelease());
			environmentReleaseRepository.save(newEnvironmentRelease);

			if (duplicateOptions.isCopyEnvironmentConfig()) {
				releaseService.copyEnvironmentConfig(sourceEnvironmentRelease, newEnvironmentRelease);
			}
		}
		return destEnvironment;
	}

	@Transactional
	public Environment move(Long environmentId, MoveOptions moveOptions) {
		Environment environment = environmentRepository.findById(environmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment: " + environmentId));

		Cluster cluster = clusterRepository.findById(moveOptions.getDestClusterId())
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destination cluster: " + moveOptions.getDestClusterId()));

		log.info("Moving environment: {} to destination cluster {}", environment, cluster);

		Map<EnvironmentReleaseId, ReleaseVersion> previouslyDeployedReleases = undeployReleasesFrom(environment);

		environment.setCluster(cluster);
		environment = environmentRepository.save(environment);

		if (moveOptions.isRedeployReleases() && !previouslyDeployedReleases.isEmpty()) {
			deployReleasesTo(environment, previouslyDeployedReleases);
		}

		return environment;
	}

	@Transactional
	public void updateOrderIndex(Long environmentId, Double orderIndex) {
		environmentRepository.updateOrderIndex(environmentId, orderIndex);
	}

	private Map<EnvironmentReleaseId, ReleaseVersion> undeployReleasesFrom(Environment environment) {
		Map<EnvironmentReleaseId, ReleaseVersion> previouslyDeployedReleases = new HashMap<>();

		log.info("Checking if there are deployed releases in {}", environment.getKey());
		environment.getEnvironmentReleases().forEach((environmentRelease) -> {
			if (environmentRelease.hasCurrentReleaseVersion()) {
				log.info("Release {} is currently deployed in {}, undeploying...", environmentRelease.getRelease().getName(), environment.getKey());

				previouslyDeployedReleases.put(environmentRelease.getId(), environmentRelease.getCurrentReleaseVersion());

				try {
					releaseService.undeploy(environmentRelease.getId());
				} catch (Exception e) {
					throw new EnvironmentException("Failed to undeploy " + environmentRelease + " from " + environment, e);
				}
			}
		});

		return previouslyDeployedReleases;
	}

	private void deployReleasesTo(Environment environment, Map<EnvironmentReleaseId, ReleaseVersion> previouslyDeployedReleases) {
		environment.getEnvironmentReleases().forEach((environmentRelease) -> {
			if (previouslyDeployedReleases.containsKey(environmentRelease.getId())) {
				ReleaseVersion releaseVersion = previouslyDeployedReleases.get(environmentRelease.getId());

				log.debug("Release {}:{} was previously deployed in {}, redeploying...", environmentRelease.getRelease().getName(), releaseVersion.getVersion(), environment.getKey());

				try {
					String message = String.format("Redeployed %s:%s to %s in %s after moving environment to new cluster",
						environmentRelease.getRelease().getName(),
						releaseVersion.getVersion(),
						environment.getKey(),
						environment.getCluster().getName());

					DeployOptions deployOptions = new DeployOptions(releaseVersion.getId(), message);
					releaseService.deploy(environmentRelease.getId(), deployOptions);
					log.info(message);
				} catch (Exception e) {
					throw new EnvironmentException("Failed to deploy " + environmentRelease + " to " + environment, e);
				}
			}
		});
	}
}
