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

package za.co.lsd.ahoy.server.task.deploy;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.ReleaseService;
import za.co.lsd.ahoy.server.task.Task;

@Component
public class DeployTask implements Task<DeployTaskContext> {
	private final ReleaseService releaseService;

	public DeployTask(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@Override
	public String getName() {
		return "deploy";
	}

	@Override
	public void execute(DeployTaskContext context) {
		releaseService.deploy(context.getEnvironmentReleaseId(), context.getDeployOptions());
	}
}
