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

package za.co.lsd.ahoy.server.helm;

import org.yaml.snakeyaml.Yaml;
import za.co.lsd.ahoy.server.applications.Application;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HelmUtils {
	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private HelmUtils() {
	}

	public static void dump(Object data, Path path, Yaml yaml) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, CHARSET)) {
			yaml.dump(data, writer);
		}
	}

	public static void dump(String data, Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, CHARSET)) {
			writer.write(data);
		}
	}

	public static String valuesName(Application application) {
		// TODO; we need to handle all special characters that may be in the app name that the values file won't accept
		return application.getName().replace("-", "");
	}
}
