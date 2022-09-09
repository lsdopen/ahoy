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

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
@ToString
public class TaskProgressEvent extends ApplicationEvent {
	private final String id;
	private final State state;
	private final String status;
	private final String message;
	@ToString.Exclude
	private final String trace;
	private final LocalDateTime time;

	public TaskProgressEvent(Object source, String id, State state, String status, String message, String trace) {
		super(source);
		this.id = id;
		this.state = state;
		this.status = status;
		this.message = message;
		this.trace = trace;
		this.time = LocalDateTime.now();
	}

	public static TaskProgressEvent createWaiting(Object source, String id, String status) {
		return new TaskProgressEvent(source, id, State.WAITING, status, null, null);
	}

	public static TaskProgressEvent createInProgressUpdate(Object source, String id, String status, String message) {
		return new TaskProgressEvent(source, id, State.IN_PROGRESS, status, message, null);
	}

	public static TaskProgressEvent createNotification(Object source, String id, String status, String message) {
		return new TaskProgressEvent(source, id, State.NOTIFICATION, status, message, null);
	}

	public static TaskProgressEvent createDone(Object source, String id) {
		return new TaskProgressEvent(source, id, State.DONE, null, null, null);
	}

	public static TaskProgressEvent createError(Object source, String id, String status, String trace) {
		return new TaskProgressEvent(source, id, State.ERROR, status, null, trace);
	}

	public enum State {
		WAITING,
		IN_PROGRESS,
		NOTIFICATION,
		DONE,
		ERROR
	}
}
