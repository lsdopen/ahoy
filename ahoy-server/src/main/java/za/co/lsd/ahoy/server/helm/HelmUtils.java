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
		return application.getName().replaceAll("-", "");
	}
}
