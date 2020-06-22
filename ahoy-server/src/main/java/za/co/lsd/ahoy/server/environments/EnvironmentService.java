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

package za.co.lsd.ahoy.server.environments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.lsd.ahoy.server.ReleaseService;
import za.co.lsd.ahoy.server.clustermanager.ClusterManager;
import za.co.lsd.ahoy.server.clustermanager.ClusterManagerFactory;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;

import java.util.List;

@Service
@RepositoryEventHandler
@Slf4j
public class EnvironmentService {
	private final EnvironmentRepository environmentRepository;
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ReleaseService releaseService;
	private final ClusterManagerFactory clusterManagerFactory;

	public EnvironmentService(EnvironmentRepository environmentRepository, EnvironmentReleaseRepository environmentReleaseRepository, ReleaseService releaseService, ClusterManagerFactory clusterManagerFactory) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.releaseService = releaseService;
		this.clusterManagerFactory = clusterManagerFactory;
	}

	@Transactional
	public Environment create(Environment environment) {
		log.info("Creating environment: {}", environment);

		Environment savedEnvironment = environmentRepository.save(environment);

		ClusterManager clusterManager = clusterManagerFactory.newManager(savedEnvironment.getCluster());
		clusterManager.createEnvironment(savedEnvironment);
		return savedEnvironment;
	}

	@Transactional
	public Environment destroy(Long environmentId) {
		Environment environment = environmentRepository.findById(environmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment: " + environmentId));

		return destroy(environment);
	}

	@Transactional
	public Environment destroy(Environment environment) {
		log.info("Destroying environment: {}", environment);

		undeployReleasesFrom(environment);

		ClusterManager clusterManager = clusterManagerFactory.newManager(environment.getCluster());
		clusterManager.deleteEnvironment(environment);

		environmentRepository.delete(environment);
		return environment;
	}

	@Transactional
	public Environment duplicate(Long sourceEnvironmentId, Long destEnvironmentId) {
		Environment sourceEnvironment = environmentRepository.findById(sourceEnvironmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find source environment: " + sourceEnvironmentId));

		Environment destEnvironment = environmentRepository.findById(destEnvironmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find destination environment: " + destEnvironmentId));

		log.info("Duplicating environment {} to environment {}", sourceEnvironment, destEnvironment);

		List<EnvironmentRelease> sourceEnvironmentReleases = sourceEnvironment.getEnvironmentReleases();
		for (EnvironmentRelease sourceEnvironmentRelease : sourceEnvironmentReleases) {

			EnvironmentRelease newEnvironmentRelease = new EnvironmentRelease();
			newEnvironmentRelease.setId(new EnvironmentReleaseId());
			newEnvironmentRelease.setRelease(sourceEnvironmentRelease.getRelease());
			newEnvironmentRelease.setEnvironment(destEnvironment);

			environmentReleaseRepository.save(newEnvironmentRelease);
		}
		return destEnvironment;
	}

	private void undeployReleasesFrom(Environment environment) {
		log.info("Checking if there are deployed releases in {}", environment.getName());
		environment.getEnvironmentReleases().forEach((environmentRelease) -> {
			if (environmentRelease.hasCurrentReleaseVersion()) {
				log.info("{} is currently deployed in {}, undeploying...", environmentRelease.getRelease().getName(), environment.getName());
				try {
					releaseService.undeploy(environmentRelease).get();
				} catch (Exception e) {
					throw new EnvironmentException("Failed to undeploy " + environmentRelease + " from " + environment, e);
				}
			}
		});
	}
}
