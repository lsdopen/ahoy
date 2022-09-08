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

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class DelegatingTaskSecurityContextRunnable implements Runnable {
	private final Runnable task;
	private final TaskContext taskContext;

	public DelegatingTaskSecurityContextRunnable(Runnable task, TaskContext taskContext) {
		this.task = Objects.requireNonNull(task);
		this.taskContext = Objects.requireNonNull(taskContext);
	}

	@Override
	public void run() {
		try {
			if (taskContext.getSecurityContext() != null) {
				SecurityContextHolder.setContext(taskContext.getSecurityContext());
			}
			task.run();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
