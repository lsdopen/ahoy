package za.co.lsd.ahoy.server.helm;

import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;
import za.co.lsd.ahoy.server.releases.ReleaseVersion;

import java.io.IOException;
import java.nio.file.Path;

public class NoopTemplateWriter implements TemplateWriter {

	@Override
	public void writeTemplates(EnvironmentRelease environmentRelease, ReleaseVersion releaseVersion, Path templatesPath) throws IOException {
	}
}
