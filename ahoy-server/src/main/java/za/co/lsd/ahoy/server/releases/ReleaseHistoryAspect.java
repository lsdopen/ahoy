package za.co.lsd.ahoy.server.releases;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import java.time.LocalDateTime;
import java.util.concurrent.Future;

import static za.co.lsd.ahoy.server.releases.ReleaseHistoryAction.*;
import static za.co.lsd.ahoy.server.releases.ReleaseHistoryStatus.*;

@Component
@Aspect
@Slf4j
public class ReleaseHistoryAspect {
	private final ReleaseHistoryService releaseHistoryService;

	public ReleaseHistoryAspect(ReleaseHistoryService releaseHistoryService) {
		this.releaseHistoryService = releaseHistoryService;
	}

	@Around("execution(* *..ReleaseService.deploy(..))")
	public Object deploy(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		EnvironmentRelease environmentRelease = (EnvironmentRelease) args[0];
		ReleaseVersion releaseVersion = (ReleaseVersion) args[1];

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
		EnvironmentRelease environmentRelease = (EnvironmentRelease) args[0];
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
