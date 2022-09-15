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
import org.springframework.security.core.context.SecurityContext;

import java.util.UUID;

@Data
public abstract class TaskContext {
	private final String id;
	private SecurityContext securityContext;

	protected TaskContext() {
		this(UUID.randomUUID().toString());
	}

	protected TaskContext(String id) {
		this.id = id;
	}

	protected SecurityContext getSecurityContext() {
		return securityContext;
	}

	protected void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	/**
	 * Set whether the task framework should send progress events for this task context.
	 *
	 * @return boolean true to send progress events; false otherwise
	 */
	public boolean sendProgress() {
		return true;
	}

	public abstract String getMessage();
}
