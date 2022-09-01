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

package za.co.lsd.ahoy.server.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.security.Role;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SecuredTask implements Task<TaskContext> {
	private CountDownLatch executedLatch;

	@Override
	public String getName() {
		return "secured-task";
	}

	public void setExecutedLatch(CountDownLatch executedLatch) {
		this.executedLatch = executedLatch;
	}

	@Override
	@Secured(Role.admin)
	public void execute(TaskContext context) {
		log.info("Executed secured task: " + context.getId());
		executedLatch.countDown();
	}

	public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
		return executedLatch.await(timeout, unit);
	}
}
