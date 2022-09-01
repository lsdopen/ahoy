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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service to publish task progress events to interested listeners.
 * <p>
 * This service has methods to start, stop and publish updated progress events.
 * The start will attach the task's context to a {@link ThreadLocal} and until stopped by done or error, task updates will be published
 * for the context attached to the same thread, therefore this service assumes a single task runs on a single thread, although
 * multiple tasks can run across multiple threads since their contexts are attached to the ThreadLocal variable.
 */
@Service
public class TaskProgressService {
	private final ThreadLocal<TaskContext> threadTaskContext = new ThreadLocal<>();
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void start(TaskContext context, String status, String message) {
		threadTaskContext.set(context);
		eventPublisher.publishEvent(TaskProgressEvent.createInProgressUpdate(this, context.getId(), status, message));
	}

	public void progress(String status, String message) {
		TaskContext context = threadTaskContext.get();
		eventPublisher.publishEvent(TaskProgressEvent.createInProgressUpdate(this, context.getId(), status, message));
	}

	public void progress(String message) {
		TaskContext context = threadTaskContext.get();
		eventPublisher.publishEvent(TaskProgressEvent.createInProgressUpdate(this, context.getId(), null, message));
	}

	public void notify(String status, String message) {
		TaskContext context = threadTaskContext.get();
		eventPublisher.publishEvent(TaskProgressEvent.createNotification(this, context.getId(), status, message));
	}

	public void error(Throwable t) {
		TaskContext context = threadTaskContext.get();
		String trace = ExceptionUtils.getStackTrace(t);
		eventPublisher.publishEvent(TaskProgressEvent.createError(this, context.getId(), t.getMessage(), trace));
		threadTaskContext.remove();
	}

	public void done() {
		TaskContext context = threadTaskContext.get();
		eventPublisher.publishEvent(TaskProgressEvent.createDone(this, context.getId()));
		threadTaskContext.remove();
	}
}
