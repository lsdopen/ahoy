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

package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import za.co.lsd.ahoy.server.argocd.model.HealthStatus;
import za.co.lsd.ahoy.server.environments.EnvironmentProjection;
import za.co.lsd.ahoy.server.releases.ReleaseProjection;
import za.co.lsd.ahoy.server.releases.ReleaseVersionProjection;

@Projection(name = "environmentRelease", types = {EnvironmentRelease.class})
public interface EnvironmentReleaseProjection {

	EnvironmentReleaseId getId();

	ReleaseProjection getRelease();

	EnvironmentProjection getEnvironment();

	ReleaseVersionProjection getCurrentReleaseVersion();

	ReleaseVersionProjection getPreviousReleaseVersion();

	@Value("#{target.latestReleaseVersion()}")
	ReleaseVersionProjection getLatestReleaseVersion();

	@Value("#{target.hasCurrentReleaseVersion()}")
	Boolean getDeployed();

	HealthStatus.StatusCode getStatus();

	Integer getApplicationsReady();
}
