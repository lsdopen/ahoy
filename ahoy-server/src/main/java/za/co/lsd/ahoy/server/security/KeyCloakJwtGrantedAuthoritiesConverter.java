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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;

public class KeyCloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
	private static final String ROLE_PREFIX = "ROLE_";
	private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
		if (authorities != null && jwt.containsClaim("resource_access")) {
			JSONObject resource_access = jwt.getClaim("resource_access");
			if (resource_access.containsKey("ahoy")) {
				JSONObject ahoy = (JSONObject) resource_access.get("ahoy");
				if (ahoy.containsKey("roles")) {
					List<Object> roles = (JSONArray) ahoy.get("roles");
					roles.forEach((role) -> authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role)));
				}
			}
		}
		return authorities;
	}
}
