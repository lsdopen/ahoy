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

package za.co.lsd.ahoy.server.applications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import za.co.lsd.ahoy.server.util.JsonProcessingRuntimeException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
@Slf4j
public class ApplicationEnvironmentSpecConverter implements AttributeConverter<ApplicationEnvironmentSpec, String> {
	private final ObjectMapper objectMapper;

	public ApplicationEnvironmentSpecConverter() {
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public String convertToDatabaseColumn(ApplicationEnvironmentSpec attribute) {
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new JsonProcessingRuntimeException("Failed to convert Application environment config document", e);
		}
	}

	@Override
	public ApplicationEnvironmentSpec convertToEntityAttribute(String dbData) {
		try {
			return objectMapper.readValue(dbData, ApplicationEnvironmentSpec.class);
		} catch (JsonProcessingException e) {
			throw new JsonProcessingRuntimeException("Failed to read Application environment config document", e);
		}
	}
}
