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

import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Data
public class TaskContext {
	private final String id;
	private ProgressMessages progressMessages;

	protected TaskContext(@Nullable ProgressMessages progressMessages) {
		this.id = UUID.randomUUID().toString();
		this.progressMessages = progressMessages;
	}

	/**
	 * Returns whether this context has progress messages or not.
	 * If there is progress messages, then it is safe to assume that progress events should be sent for the task.
	 *
	 * @return boolean true has progress messages; false otherwise
	 */
	public boolean hasProgressMessages() {
		return progressMessages != null;
	}

	public ProgressMessages getProgressMessages() {
		return progressMessages;
	}
}
