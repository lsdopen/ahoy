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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import za.co.lsd.ahoy.server.argocd.model.ArgoEvents;
import za.co.lsd.ahoy.server.argocd.model.Resource;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentReleaseId;
import za.co.lsd.ahoy.server.releases.Release;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;
import za.co.lsd.ahoy.server.releases.resources.ResourceNode;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.task.TaskExecutor;
import za.co.lsd.ahoy.server.util.SseEmitterSubscriber;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api")
@Slf4j
@Secured({Role.admin, Role.releasemanager})
public class ReleaseController {
	private final ReleaseService releaseService;
	private final TaskExecutor taskExecutor;

	private ExecutorService sseMvcExecutor;

	public ReleaseController(ReleaseService releaseService, TaskExecutor taskExecutor) {
		this.releaseService = releaseService;
		this.taskExecutor = taskExecutor;
	}

	@Autowired
	public void setSseMvcExecutor(ExecutorService sseMvcExecutor) {
		this.sseMvcExecutor = sseMvcExecutor;
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/deploy")
	public ListenableFuture<EnvironmentRelease> deploy(@PathVariable EnvironmentReleaseId environmentReleaseId,
													   @RequestBody DeployOptions deployOptions) {

		return taskExecutor.executeSync(() -> releaseService.deploy(environmentReleaseId, deployOptions), deployOptions.getProgressMessages());
	}

	@PostMapping("/environmentReleases/{environmentReleaseId}/undeploy")
	public ListenableFuture<EnvironmentRelease> undeploy(@PathVariable EnvironmentReleaseId environmentReleaseId,
														 @RequestBody UndeployOptions undeployOptions) {

		return taskExecutor.executeAsync(() -> releaseService.undeploy(environmentReleaseId), undeployOptions.getProgressMessages());
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
	public ListenableFuture<EnvironmentRelease> remove(@PathVariable EnvironmentReleaseId environmentReleaseId,
													   @RequestBody RemoveOptions removeOptions) {

		return taskExecutor.executeAsync(() -> releaseService.remove(environmentReleaseId), removeOptions.getProgressMessages());
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

	@PostMapping("/releases/duplicate/{sourceReleaseId}/{destReleaseId}")
	public ResponseEntity<Release> duplicate(@PathVariable Long sourceReleaseId, @PathVariable Long destReleaseId,
											 @RequestBody DuplicateOptions duplicateOptions) {

		Release duplicatedRelease = releaseService.duplicate(sourceReleaseId, destReleaseId, duplicateOptions);
		return new ResponseEntity<>(duplicatedRelease, new HttpHeaders(), HttpStatus.OK);
	}

	@GetMapping("/environmentReleases/{environmentReleaseId}/resources")
	@Secured({Role.admin, Role.releasemanager, Role.developer})
	public ResponseEntity<ResourceNode> resources(@PathVariable EnvironmentReleaseId environmentReleaseId) {

		Optional<ResourceNode> resourceNode = releaseService.getResources(environmentReleaseId);
		return new ResponseEntity<>(resourceNode.orElse(null), new HttpHeaders(), HttpStatus.OK);
	}

	@GetMapping("/environmentReleases/{environmentReleaseId}/resource")
	@Secured({Role.admin, Role.releasemanager, Role.developer})
	public ResponseEntity<Resource> resource(@PathVariable EnvironmentReleaseId environmentReleaseId,
											 @RequestParam String resourceNamespace,
											 @RequestParam String resourceName,
											 @RequestParam String version,
											 @RequestParam String kind) {

		Optional<Resource> resource = releaseService.getResource(environmentReleaseId, resourceNamespace, resourceName, version, kind);
		return new ResponseEntity<>(resource.orElse(null), new HttpHeaders(), HttpStatus.OK);
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

	@GetMapping(value = "/environmentReleases/{environmentReleaseId}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Secured({Role.admin, Role.releasemanager, Role.developer})
	public SseEmitter logs(@PathVariable EnvironmentReleaseId environmentReleaseId,
						   @RequestParam String podName,
						   @RequestParam String resourceNamespace,
						   @RequestParam String container) {

		Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
		sseMvcExecutor.execute(() -> {
			SecurityContextHolder.getContext().setAuthentication(originalAuth);
			releaseService.getLogs(environmentReleaseId, podName, resourceNamespace, container).subscribe(new SseEmitterSubscriber<>(emitter));
		});
		return emitter;
	}
}
