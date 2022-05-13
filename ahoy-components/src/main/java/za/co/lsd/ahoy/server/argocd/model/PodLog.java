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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodLog {
	private Result result;
	private Error error;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Result {
		private String content;
		private Boolean last;
		private String podName;
		private Date timeStamp;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Error {
		private Integer http_code;
		private String http_status;
		private String message;
	}
}
