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

package za.co.lsd.ahoy.server.releases;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.DeployOptions;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseRepository;

import java.time.LocalDateTime;
import java.util.concurrent.Future;

import static za.co.lsd.ahoy.server.releases.ReleaseHistoryAction.*;
import static za.co.lsd.ahoy.server.releases.ReleaseHistoryStatus.*;

@Component
@Aspect
@Slf4j
public class ReleaseHistoryAspect {
	private final EnvironmentReleaseRepository environmentReleaseRepository;
	private final ReleaseVersionRepository releaseVersionRepository;
	private final ReleaseHistoryService releaseHistoryService;

	public ReleaseHistoryAspect(EnvironmentReleaseRepository environmentReleaseRepository, ReleaseVersionRepository releaseVersionRepository, ReleaseHistoryService releaseHistoryService) {
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
			.environmentRelease(environmentRelease)
			.releaseVersion(releaseVersion);

		try {
			Future<EnvironmentRelease> deployedEnvironmentRelease = (Future<EnvironmentRelease>) pjp.proceed();

			releaseHistoryService.save(historyBuilder
				.status(SUCCESS)
				.description("Deployment successful")
				.build());

			return deployedEnvironmentRelease;
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
			.environmentRelease(environmentRelease)
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
}
