package za.co.lsd.ahoy.server.settings;

import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.argocd.ArgoSettings;
import za.co.lsd.ahoy.server.argocd.ArgoSettingsRepository;
import za.co.lsd.ahoy.server.docker.DockerSettings;
import za.co.lsd.ahoy.server.docker.DockerSettingsRepository;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.git.GitSettingsRepository;

@Component
public class DbSettingsProvider implements SettingsProvider {
	private final GitSettingsRepository gitSettingsRepository;
	private final ArgoSettingsRepository argoSettingsRepository;
	private final DockerSettingsRepository dockerSettingsRepository;

	public DbSettingsProvider(GitSettingsRepository gitSettingsRepository,
	                          ArgoSettingsRepository argoSettingsRepository,
	                          DockerSettingsRepository dockerSettingsRepository) {
		this.gitSettingsRepository = gitSettingsRepository;
		this.argoSettingsRepository = argoSettingsRepository;
		this.dockerSettingsRepository = dockerSettingsRepository;
	}

	@Override
	public GitSettings getGitSettings() {
		return gitSettingsRepository.findById(1L).orElse(new GitSettings());
	}

	@Override
	public ArgoSettings getArgoSettings() {
		return argoSettingsRepository.findById(1L).orElse(new ArgoSettings());
	}

	@Override
	public DockerSettings getDockerSettings() {
		return dockerSettingsRepository.findById(1L).orElse(new DockerSettings());
	}
}
