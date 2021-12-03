/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@Profile("keycloak")
public class KeyCloakSecurityConfiguration {

	@Bean
	public JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakJwtGrantedAuthoritiesConverter());
		return jwtAuthenticationConverter;
	}

	@Bean
	public KeyCloakJwtGrantedAuthoritiesConverter keycloakJwtGrantedAuthoritiesConverter() {
		return new KeyCloakJwtGrantedAuthoritiesConverter();
	}

	@Bean
	public String rolesTokenPath() {
		return "resource_access.ahoy.roles";
	}
}
