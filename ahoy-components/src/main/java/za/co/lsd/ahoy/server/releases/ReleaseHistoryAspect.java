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

package za.co.lsd.ahoy.server.releases;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;
import za.co.lsd.ahoy.server.environments.Environment;
import za.co.lsd.ahoy.server.environments.EnvironmentRepository;
import za.co.lsd.ahoy.server.release.DeployOptions;
import za.co.lsd.ahoy.server.release.PromoteOptions;
import za.co.lsd.ahoy.server.release.UpgradeOptions;

import java.time.LocalDateTime;

import static za.co.lsd.ahoy.server.releases.ReleaseHistoryAction.*;
import static za.co.lsd.ahoy.server.releases.ReleaseHistoryStatus.*;

@Component
@Aspect
@Slf4j
public class ReleaseHistoryAspect {
	private final EnvironmentRepository environmentRepository;
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ReleaseVersionRepository releaseVersionRepository;
	private final ReleaseHistoryService releaseHistoryService;

	public ReleaseHistoryAspect(EnvironmentRepository environmentRepository,
								EnvironmentReleaseRepository environmentReleaseRepository,
								ReleaseVersionRepository releaseVersionRepository,
								ReleaseHistoryService releaseHistoryService) {
		this.environmentRepository = environmentRepository;
		this.environmentReleaseRepository = environmentReleaseRepository;
		this.releaseVersionRepository = releaseVersionRepository;
		this.releaseHistoryService = releaseHistoryService;
	}

	@Around("execution(* *..ReleaseService.deploy(..))")
	public Object deploy(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		EnvironmentReleaseId environmentReleaseId = (EnvironmentReleaseId) args[0];
		DeployOptions deployOptions = (DeployOptions) args[1];

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));
		ReleaseVersion releaseVersion = releaseVersionRepository.findById(deployOptions.getReleaseVersionId())
			.orElseThrow(() -> new ResourceNotFoundException("Could not find releaseVersion in release, releaseVersionId: " + deployOptions.getReleaseVersionId()));

		ReleaseHistory.ReleaseHistoryBuilder historyBuilder = ReleaseHistory.builder()
			.action(DEPLOY)
			.time(LocalDateTime.now())
			.environment(environmentRelease.getEnvironment())
			.release(environmentRelease.getRelease())
			.releaseVersion(releaseVersion);

		try {
			Object retVal = pjp.proceed();

			releaseHistoryService.save(historyBuilder
				.status(SUCCESS)
				.description("Deployment successful")
				.build());

			return retVal;
		} catch (Throwable t) {
			log.error("Failed to save report history item", t);

			releaseHistoryService.save(historyBuilder
				.status(FAILED)
				.description("Deployment failed: \n" + t.getMessage())
				.build());

			throw t;
		}
	}

	@Around("execution(* *..ReleaseService.undeploy(..))")
	public Object undeploy(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		EnvironmentReleaseId environmentReleaseId = (EnvironmentReleaseId) args[0];

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		ReleaseVersion releaseVersion = environmentRelease.getCurrentReleaseVersion();

		ReleaseHistory.ReleaseHistoryBuilder historyBuilder = ReleaseHistory.builder()
			.action(UNDEPLOY)
			.time(LocalDateTime.now())
			.environment(environmentRelease.getEnvironment())
			.release(environmentRelease.getRelease())
			.releaseVersion(releaseVersion);

		try {
			Object retVal = pjp.proceed();

			releaseHistoryService.save(historyBuilder
				.status(SUCCESS)
				.description("Un-deployment successful")
				.build());

			return retVal;
		} catch (Throwable t) {

			releaseHistoryService.save(historyBuilder
				.status(FAILED)
				.description("Un-deployment failed: \n" + t.getMessage())
				.build());

			throw t;
		}
	}

	@Around("execution(* *..ReleaseService.upgrade(..))")
	public Object upgrade(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		Long releaseVersionId = (Long) args[0];
		UpgradeOptions upgradeOptions = (UpgradeOptions) args[1];

		ReleaseVersion releaseVersion = releaseVersionRepository.findById(releaseVersionId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find releaseVersion in release, releaseVersionId: " + releaseVersionId));

		ReleaseHistory.ReleaseHistoryBuilder historyBuilder = ReleaseHistory.builder()
			.action(UPGRADE)
			.release(releaseVersion.getRelease())
			.releaseVersion(releaseVersion)
			.time(LocalDateTime.now());

		try {
			Object retVal = pjp.proceed();

			releaseHistoryService.save(historyBuilder
				.status(SUCCESS)
				.description("Upgrade to version " + upgradeOptions.getVersion() + " successful")
				.build());

			return retVal;
		} catch (Throwable t) {

			releaseHistoryService.save(historyBuilder
				.status(FAILED)
				.description("Upgrade to version " + upgradeOptions.getVersion() + " failed: \n" + t.getMessage())
				.build());

			throw t;
		}
	}

	@Around("execution(* *..ReleaseService.promote(..))")
	public Object promote(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		EnvironmentReleaseId environmentReleaseId = (EnvironmentReleaseId) args[0];
		PromoteOptions promoteOptions = (PromoteOptions) args[1];

		EnvironmentRelease environmentRelease = environmentReleaseRepository.findById(environmentReleaseId)
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment release: " + environmentReleaseId));

		Environment destEnvironment = environmentRepository.findById(promoteOptions.getDestEnvironmentId())
			.orElseThrow(() -> new ResourceNotFoundException("Could not find environment: " + promoteOptions.getDestEnvironmentId()));

		ReleaseHistory.ReleaseHistoryBuilder historyBuilder = ReleaseHistory.builder()
			.action(PROMOTE)
			.environment(environmentRelease.getEnvironment())
			.release(environmentRelease.getRelease())
			.time(LocalDateTime.now());

		try {
			Object retVal = pjp.proceed();

			releaseHistoryService.save(historyBuilder
				.status(SUCCESS)
				.description("Promote to environment " + destEnvironment.getKey() + " successful")
				.build());

			return retVal;
		} catch (Throwable t) {

			releaseHistoryService.save(historyBuilder
				.status(FAILED)
				.description("Promote to environment " + destEnvironment.getKey() + " failed: \n" + t.getMessage())
				.build());

			throw t;
		}
	}
}
