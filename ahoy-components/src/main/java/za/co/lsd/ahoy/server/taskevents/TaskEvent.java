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

package za.co.lsd.ahoy.server.taskevents;

import lombok.Data;
import za.co.lsd.ahoy.server.ReleaseStatusChangedEvent;
import za.co.lsd.ahoy.server.argocd.ArgoConnectionEvent;
import za.co.lsd.ahoy.server.task.TaskProgressEvent;

@Data
public class TaskEvent {
	private ReleaseStatusChangedEvent releaseStatusChangedEvent;
	private ArgoConnectionEvent argoConnectionEvent;
	private TaskProgressEvent taskProgressEvent;

	public TaskEvent(ReleaseStatusChangedEvent releaseStatusChangedEvent) {
		this.releaseStatusChangedEvent = releaseStatusChangedEvent;
	}

	public TaskEvent(ArgoConnectionEvent argoConnectionEvent) {
		this.argoConnectionEvent = argoConnectionEvent;
	}

	public TaskEvent(TaskProgressEvent taskProgressEvent) {
		this.taskProgressEvent = taskProgressEvent;
	}
}
