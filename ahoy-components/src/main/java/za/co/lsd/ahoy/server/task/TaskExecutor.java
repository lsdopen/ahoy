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
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
public class TaskExecutor {
	private final AsyncListenableTaskExecutor synchronousTaskExecutor;
	private final AsyncListenableTaskExecutor asynchronousTaskExecutor;

	private final TaskProgressService taskProgressService;

	public TaskExecutor(AsyncListenableTaskExecutor synchronousTaskExecutor, AsyncListenableTaskExecutor asynchronousTaskExecutor, TaskProgressService taskProgressService) {
		this.synchronousTaskExecutor = synchronousTaskExecutor;
		this.asynchronousTaskExecutor = asynchronousTaskExecutor;
		this.taskProgressService = taskProgressService;
	}

	public <R> ListenableFuture<R> executeSync(Task<R> task, ProgressMessages progressMessages) {
		return execute(synchronousTaskExecutor, task, progressMessages);
	}

	public <R> ListenableFuture<R> executeAsync(Task<R> task, ProgressMessages progressMessages) {
		return execute(asynchronousTaskExecutor, task, progressMessages);
	}

	private <R> ListenableFuture<R> execute(AsyncListenableTaskExecutor taskExecutor, Task<R> task, ProgressMessages progressMessages) {
		TaskContext context = new TaskContext(progressMessages);

		taskProgressService.waiting(context, "waiting");

		ListenableFuture<R> future = taskExecutor.submitListenable(() -> {
			taskProgressService.start(context, "preparing");
			return task.execute();
		});

		future.addCallback(new ListenableFutureCallback<>() {
			@Override
			public void onFailure(Throwable ex) {
				taskProgressService.error(ex);
			}

			@Override
			public void onSuccess(R result) {
				taskProgressService.done();
			}
		});
		return future;
	}
}
