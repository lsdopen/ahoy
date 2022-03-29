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

package za.co.lsd.ahoy.server.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.util.JsonProcessingRuntimeException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.HashMap;

@Converter
@Slf4j
public class SettingsConverter implements AttributeConverter<BaseSettings, String> {
	private final ObjectMapper objectMapper;

	public SettingsConverter() {
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public String convertToDatabaseColumn(BaseSettings settings) {
		try {
			return objectMapper.writeValueAsString(settings);
		} catch (JsonProcessingException e) {
			throw new JsonProcessingRuntimeException("Failed to convert settings document", e);
		}
	}

	@Override
	public BaseSettings convertToEntityAttribute(String dbData) {
		try {
			HashMap<String, Object> settingsMap = objectMapper.readValue(dbData, new TypeReference<>() {
			});
			switch (Settings.Type.valueOf(settingsMap.get("type").toString())) {
				case GIT:
					return objectMapper.readValue(dbData, GitSettings.class);
				case ARGO:
					return objectMapper.readValue(dbData, ArgoSettings.class);
				case DOCKER:
					return objectMapper.readValue(dbData, DockerSettings.class);
				default:
					throw new JsonProcessingRuntimeException("Unknown settings type");
			}

		} catch (JsonProcessingException e) {
			throw new JsonProcessingRuntimeException("Failed to read settings document", e);
		}
	}
}
