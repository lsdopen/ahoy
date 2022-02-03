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

package za.co.lsd.ahoy.server.argocd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgoEvents {
	private List<Event> items;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Event {
		private String action;
		private Long count;
		private Date eventTime;
		private Date firstTimestamp;
		private Date lastTimestamp;
		private String message;
		private String reason;
		private Source source;
		private InvolvedObject involvedObject;
		private String type;

		@Data
		@Builder
		@NoArgsConstructor
		@AllArgsConstructor
		public static class Source {
			private String component;
			private String host;
		}

		@Data
		@Builder
		@NoArgsConstructor
		@AllArgsConstructor
		public static class InvolvedObject {
			private String apiVersion;
			private String fieldPath;
			private String kind;
			private String name;
			private String namespace;
			private String resourceVersion;
			private String uid;
		}
	}
}
