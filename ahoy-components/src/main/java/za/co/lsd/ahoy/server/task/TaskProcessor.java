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
import org.springframework.security.core.context.SecurityContextHolder;
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

	public TaskProcessor(TaskQueue taskQueue, TaskExecutor synchronousTaskExecutor) {
		this.taskQueue = taskQueue;
		this.synchronousTaskExecutor = synchronousTaskExecutor;
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
				TaskExecution taskExecution = taskQueue.poll(100, TimeUnit.MILLISECONDS);
				if (taskExecution != null) {
					synchronousTaskExecutor.execute(() -> {
						Task task = taskExecution.getTask();
						try {
							TaskContext context = taskExecution.getContext();
							if (context.getAuthentication() != null) {
								SecurityContextHolder.getContext().setAuthentication(context.getAuthentication());
							}
							task.execute(context);

						} catch (Throwable t) {
							log.error("Execution of task: " + task.getName() + " failed", t);

						} finally {
							SecurityContextHolder.getContext().setAuthentication(null);
						}
					});
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
