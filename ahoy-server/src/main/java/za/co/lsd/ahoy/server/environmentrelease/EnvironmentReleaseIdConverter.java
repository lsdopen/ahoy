/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.environmentrelease;

import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class EnvironmentReleaseIdConverter implements BackendIdConverter {

	@Override
	public Serializable fromRequestId(String id, Class<?> entityType) {
		if (id == null)
			return null;

		String[] parts = id.split("_");
		return new EnvironmentReleaseId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
	}

	@Override
	public String toRequestId(Serializable source, Class<?> entityType) {
		EnvironmentReleaseId id = (EnvironmentReleaseId) source;
		return String.format("%s_%s", id.getEnvironmentId(), id.getReleaseId());
	}

	@Override
	public boolean supports(Class<?> type) {
		return EnvironmentRelease.class.equals(type) || EnvironmentReleaseId.class.equals(type);
	}
}
