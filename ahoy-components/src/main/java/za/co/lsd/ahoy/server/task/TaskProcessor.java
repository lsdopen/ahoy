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
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TaskProcessor implements Runnable {
	private volatile boolean running = true;
	private final TaskQueue taskQueue;
	private final TaskExecutor synchronousTaskExecutor;
	private final TaskExecutor asynchronousTaskExecutor;
	private final TaskProgressService taskProgressService;

	public TaskProcessor(TaskQueue taskQueue, TaskExecutor synchronousTaskExecutor, TaskExecutor asynchronousTaskExecutor, TaskProgressService taskProgressService) {
		this.taskQueue = taskQueue;
		this.synchronousTaskExecutor = synchronousTaskExecutor;
		this.asynchronousTaskExecutor = asynchronousTaskExecutor;
		this.taskProgressService = taskProgressService;
	}

	@PostConstruct
	public void start() {
		Executors.newSingleThreadExecutor().execute(this);
	}

	@PreDestroy
	public void stop() {
		this.running = false;
	}

	@Override
	public void run() {
		try {
			while (running) {
				TaskExecution<?, ?> taskExecution = taskQueue.poll(100, TimeUnit.MILLISECONDS);
				if (taskExecution != null) {
					TaskContext context = taskExecution.getContext();
					taskProgressService.waiting(taskExecution.getContext(), "Waiting to " + context.getMessage());
					TaskExecutor taskExecutor = taskExecution.isAsync() ? asynchronousTaskExecutor : synchronousTaskExecutor;
					taskExecutor.execute(new DelegatingTaskSecurityContextRunnable(() -> execute(taskExecution), context));
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private <T extends Task<C>, C extends TaskContext> TaskExecution<T, C> execute(TaskExecution<T, C> taskExecution) {
		T task = taskExecution.getTask();
		C context = taskExecution.getContext();
		taskProgressService.start(context, "Preparing to " + context.getMessage(), null);
		try {
			task.execute(context);
			taskProgressService.done();

		} catch (Throwable t) {
			log.error("Execution of " + context.getMessage() + " failed", t);
			taskProgressService.error(t);
		}
		return taskExecution;
	}
}
