package za.co.lsd.ahoy.server.settings;

import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.git.GitSettings;

public interface SettingsProvider {

	GitSettings getGitSettings();

	ArgoSettings getArgoSettings();

	DockerSettings getDockerSettings();
}
