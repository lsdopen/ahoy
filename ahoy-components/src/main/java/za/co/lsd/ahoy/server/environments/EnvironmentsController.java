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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.task.TaskExecutor;

@RestController
@RequestMapping("/api/environments")
@Slf4j
@Secured({Role.admin, Role.releasemanager})
public class EnvironmentsController {
	private final EnvironmentService environmentService;
	private final TaskExecutor taskExecutor;

	public EnvironmentsController(EnvironmentService environmentService, TaskExecutor taskExecutor) {
		this.environmentService = environmentService;
		this.taskExecutor = taskExecutor;
	}

	@DeleteMapping("/delete/{environmentId}")
	public ListenableFuture<Environment> delete(@PathVariable Long environmentId,
												@RequestBody DeleteOptions deleteOptions) {

		return taskExecutor.executeAsync(() -> environmentService.delete(environmentId), deleteOptions.getProgressMessages());
	}

	@PostMapping("/duplicate/{sourceEnvironmentId}/{destEnvironmentId}")
	public ResponseEntity<Environment> duplicate(@PathVariable Long sourceEnvironmentId, @PathVariable Long destEnvironmentId,
												 @RequestBody DuplicateOptions duplicateOptions) {

		Environment destEnvironment = environmentService.duplicate(sourceEnvironmentId, destEnvironmentId, duplicateOptions);

		return new ResponseEntity<>(destEnvironment, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/{environmentId}/move")
	public ListenableFuture<Environment> move(@PathVariable Long environmentId,
											  @RequestBody MoveOptions moveOptions) {

		return taskExecutor.executeSync(() -> environmentService.move(environmentId, moveOptions), moveOptions.getProgressMessages());
	}

	@PutMapping("/{environmentId}/updateOrderIndex")
	public ResponseEntity<Environment> updateOrderIndex(@PathVariable Long environmentId,
														@RequestParam Double orderIndex) {

		environmentService.updateOrderIndex(environmentId, orderIndex);
		return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.OK);
	}
}
