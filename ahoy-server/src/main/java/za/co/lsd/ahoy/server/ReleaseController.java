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

package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import za.co.lsd.ahoy.server.argocd.model.ArgoEvents;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.releases.*;
import za.co.lsd.ahoy.server.releases.resources.ResourceNode;
import za.co.lsd.ahoy.server.security.Role;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api")
@Slf4j
@Secured({Role.admin, Role.releasemanager})
public class ReleaseController {
	private final ReleaseService releaseService;

	public ReleaseController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/deploy")
	public ResponseEntity<EnvironmentRelease> deploy(@PathVariable EnvironmentReleaseId environmentReleaseId,
													 @RequestBody DeployOptions deployOptions) throws ExecutionException, InterruptedException {

		Future<EnvironmentRelease> deployedEnvironmentRelease = releaseService.deploy(environmentReleaseId, deployOptions);
		return new ResponseEntity<>(deployedEnvironmentRelease.get(), new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/undeploy")
	public ResponseEntity<EnvironmentRelease> undeploy(@PathVariable EnvironmentReleaseId environmentReleaseId) throws ExecutionException, InterruptedException {

		Future<EnvironmentRelease> undeployedEnvironmentRelease = releaseService.undeploy(environmentReleaseId);
		return new ResponseEntity<>(undeployedEnvironmentRelease.get(), new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/promote")
	public ResponseEntity<EnvironmentRelease> promote(@PathVariable EnvironmentReleaseId environmentReleaseId,
													  @RequestBody PromoteOptions promoteOptions) {

		EnvironmentRelease promotedEnvironmentRelease = releaseService.promote(environmentReleaseId, promoteOptions);
		return new ResponseEntity<>(promotedEnvironmentRelease, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/releaseVersions/{releaseVersionId}/upgrade")
	public ResponseEntity<ReleaseVersion> upgrade(@PathVariable Long releaseVersionId,
												  @RequestBody UpgradeOptions upgradeOptions) {

		ReleaseVersion upgradedReleaseVersion = releaseService.upgrade(releaseVersionId, upgradeOptions);
		return new ResponseEntity<>(upgradedReleaseVersion, new HttpHeaders(), HttpStatus.OK);
	}

	@DeleteMapping("/environmentReleases/{environmentReleaseId}/remove")
	public ResponseEntity<EnvironmentRelease> remove(@PathVariable EnvironmentReleaseId environmentReleaseId) throws ExecutionException, InterruptedException {

		Future<EnvironmentRelease> removedEnvironmentRelease = releaseService.remove(environmentReleaseId);
		return new ResponseEntity<>(removedEnvironmentRelease.get(), new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/copyEnvConfig")
	public ResponseEntity<EnvironmentRelease> copyEnvConfig(@PathVariable EnvironmentReleaseId environmentReleaseId,
															@RequestParam Long sourceReleaseVersionId,
															@RequestParam Long destReleaseVersionId) {

		EnvironmentRelease environmentRelease = releaseService.copyEnvConfig(environmentReleaseId, sourceReleaseVersionId, destReleaseVersionId);
		return new ResponseEntity<>(environmentRelease, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/releaseVersions/{releaseVersionId}/copyAppEnvConfig")
	public ResponseEntity<Void> copyApplicationVersionEnvConfig(@PathVariable Long releaseVersionId,
																@RequestParam Long sourceApplicationVersionId,
																@RequestParam Long destApplicationVersionId) {

		releaseService.copyApplicationVersionEnvConfig(releaseVersionId, sourceApplicationVersionId, destApplicationVersionId);
		return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/releases/{releaseId}/duplicate")
	public ResponseEntity<Release> duplicate(@PathVariable Long releaseId, @RequestBody DuplicateOptions duplicateOptions) {

		Release duplicatedRelease = releaseService.duplicate(releaseId, duplicateOptions);
		return new ResponseEntity<>(duplicatedRelease, new HttpHeaders(), HttpStatus.OK);
	}

	@GetMapping("/environmentReleases/{environmentReleaseId}/resources")
	@Secured({Role.admin, Role.releasemanager, Role.developer})
	public ResponseEntity<ResourceNode> resources(@PathVariable EnvironmentReleaseId environmentReleaseId) {

		Optional<ResourceNode> resourceNode = releaseService.getResources(environmentReleaseId);
		return new ResponseEntity<>(resourceNode.orElse(null), new HttpHeaders(), HttpStatus.OK);
	}

	@GetMapping("/environmentReleases/{environmentReleaseId}/events")
	@Secured({Role.admin, Role.releasemanager, Role.developer})
	public ResponseEntity<ArgoEvents> events(@PathVariable EnvironmentReleaseId environmentReleaseId,
											 @RequestParam String resourceUid,
											 @RequestParam String resourceNamespace,
											 @RequestParam String resourceName) {

		Optional<ArgoEvents> events = releaseService.getEvents(environmentReleaseId, resourceUid, resourceNamespace, resourceName);
		return new ResponseEntity<>(events.orElse(null), new HttpHeaders(), HttpStatus.OK);
	}
}
