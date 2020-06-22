package za.co.lsd.ahoy.server.argocd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import za.co.lsd.ahoy.server.argocd.model.*;
import za.co.lsd.ahoy.server.git.GitSettings;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ArgoClient {
	private final RestTemplate restClient;
	private final SettingsProvider settingsProvider;
	private ObjectMapper objectMapper;

	public ArgoClient(RestTemplate restClient, SettingsProvider settingsProvider) {
		this.restClient = restClient;
		this.settingsProvider = settingsProvider;
	}

	@Autowired
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Optional<ArgoApplication> getApplication(String applicationName) {
		return getApplication(applicationName, false);
	}

	public Optional<ArgoApplication> getApplication(String applicationName, boolean refresh) {
		Objects.requireNonNull(applicationName, "applicationName is required");

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			ResponseEntity<ArgoApplication> get = restClient.exchange(apiPath(settings) + "/applications/" + applicationName + "?refresh=" + refresh,
				HttpMethod.GET,
				new HttpEntity<>(httpHeaders),
				ArgoApplication.class);
			ArgoApplication application = get.getBody();
			log.info("Application: {}", application);
			return Optional.ofNullable(application);

		} catch (RestClientResponseException e) {
			if (e instanceof HttpStatusCodeException) {
				HttpStatusCodeException statusCodeException = (HttpStatusCodeException) e;
				if (HttpStatus.NOT_FOUND.equals(statusCodeException.getStatusCode())) {
					return Optional.empty();
				}
			}
			String reason = getReasonMessage(e);
			log.error("Failed to get application: {}, reason: {}", applicationName, reason);
			throw new ArgoException("Failed to get application: " + applicationName + ", reason: " + reason, e);
		}
	}

	public ArgoApplication createApplication(ArgoApplication application) {
		Objects.requireNonNull(application, "application is required");
		log.info("Creating application: {}", application);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			ResponseEntity<ArgoApplication> create = restClient.exchange(apiPath(settings) + "/applications",
				HttpMethod.POST,
				new HttpEntity<>(application, httpHeaders),
				ArgoApplication.class);
			ArgoApplication createdApplication = create.getBody();
			log.info("Created application: {}", createdApplication);
			return createdApplication;

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to create application: {}, reason: {}", application.getMetadata().getName(), reason);
			throw new ArgoException("Failed to create application: " + application.getMetadata().getName() + ", reason: " + reason, e);
		}
	}

	public ArgoApplication updateApplication(ArgoApplication application) {
		Objects.requireNonNull(application, "application is required");
		log.info("Updating application: {}", application);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			ResponseEntity<ArgoApplication> update = restClient.exchange(apiPath(settings) + "/applications/" + application.getMetadata().getName(),
				HttpMethod.PUT,
				new HttpEntity<>(application, httpHeaders),
				ArgoApplication.class);
			ArgoApplication updatedApplication = update.getBody();
			log.info("Updated application: {}", updatedApplication);
			return updatedApplication;

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to update application: {}, reason: {}", application.getMetadata().getName(), reason);
			throw new ArgoException("Failed to update application: " + application.getMetadata().getName() + ", reason: " + reason, e);
		}
	}

	public void deleteApplication(String applicationName) {
		Objects.requireNonNull(applicationName, "applicationName is required");
		log.info("Deleting application: {}", applicationName);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			restClient.exchange(apiPath(settings) + "/applications/" + applicationName,
				HttpMethod.DELETE,
				new HttpEntity<>(httpHeaders),
				Void.class);
			log.info("Deleted application: {}", applicationName);

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to delete application: {}, reason: {}", applicationName, reason);
			throw new ArgoException("Failed to delete application: " + applicationName + ", reason: " + reason, e);
		}
	}

	public ArgoRepositories getRepositories() {
		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			ResponseEntity<ArgoRepositories> get = restClient.exchange(apiPath(settings) + "/repositories",
				HttpMethod.GET,
				new HttpEntity<>(httpHeaders),
				ArgoRepositories.class);
			ArgoRepositories argoRepositories = get.getBody();
			log.info("Repositories: {}", argoRepositories);
			return argoRepositories;

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to get repositories, reason: {}", reason);
			throw new ArgoException("Failed to get repositories, reason: " + reason, e);
		}
	}

	public void upsertRepository() {
		GitSettings gitSettings = settingsProvider.getGitSettings();

		ArgoRepositories repositories = getRepositories();
		Optional<ArgoRepository> existingArgoRepository = (repositories.getItems() != null) ?
			repositories.getItems().stream()
				.filter(repository -> repository.getRepo().equals(gitSettings.getRemoteRepoUri()))
				.findFirst() :
			Optional.empty();

		ArgoRepository.ArgoRepositoryBuilder argoRepositoryBuilder = ArgoRepository.builder()
			.type("git")
			.repo(gitSettings.getRemoteRepoUri());

		switch (gitSettings.getCredentials()) {
			case NONE:
				argoRepositoryBuilder.insecure(true);
				break;
			case HTTPS:
				argoRepositoryBuilder.username(gitSettings.getHttpsUsername());
				argoRepositoryBuilder.password(gitSettings.getHttpsPassword());
				break;
			case SSH:
				argoRepositoryBuilder.sshPrivateKey(gitSettings.getPrivateKey());
				break;
		}

		ArgoRepository argoRepository = argoRepositoryBuilder.build();
		if (existingArgoRepository.isEmpty()) {
			createRepository(argoRepository);

		} else {
			updateRepository(argoRepository);
		}
	}

	public void createRepository(ArgoRepository repository) {
		Objects.requireNonNull(repository, "repository is required");
		log.info("Creating repository: {}", repository);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			restClient.exchange(apiPath(settings) + "/repositories",
				HttpMethod.POST,
				new HttpEntity<>(repository, httpHeaders),
				Void.class);
			log.info("Repository created");

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to create repository: {}, reason: {}", repository.getRepo(), reason);
			throw new ArgoException("Failed to create repository: " + repository.getRepo() + ", reason: " + reason, e);
		}
	}

	public void updateRepository(ArgoRepository repository) {
		Objects.requireNonNull(repository, "repository is required");
		log.info("Updating repository: {}", repository);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			String encodedRepo = URLEncoder.encode(repository.getRepo(), StandardCharsets.UTF_8);
			restClient.exchange(apiPath(settings) + "/repositories/" + encodedRepo,
				HttpMethod.PUT,
				new HttpEntity<>(repository, httpHeaders),
				Void.class);
			log.info("Repository updated");

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to update repository: {}, reason: {}", repository.getRepo(), reason);
			throw new ArgoException("Failed to update repository: " + repository.getRepo() + ", reason: " + reason, e);
		}
	}

	public void createRepositoryCertificates() throws JSchException {
		GitSettings gitSettings = settingsProvider.getGitSettings();

		final JSch jsch = new JSch();
		String knownHosts = gitSettings.getSshKnownHosts();
		jsch.setKnownHosts(IOUtils.toInputStream(knownHosts, StandardCharsets.US_ASCII));
		HostKeyRepository hostKeyRepository = jsch.getHostKeyRepository();
		List<RepositoryCertificate> items = Arrays.stream(hostKeyRepository.getHostKey())
			.map(hostKey -> RepositoryCertificate.builder()
				.serverName(hostKey.getHost())
				.certType("ssh")
				.certData(Base64.getEncoder().encodeToString(hostKey.getKey().getBytes(StandardCharsets.US_ASCII)))
				.certSubType(hostKey.getType())
				.certInfo("")
				.build()).collect(Collectors.toList());
		createRepositoryCertificates(new RepositoryCertificates(items));
	}

	public RepositoryCertificates createRepositoryCertificates(RepositoryCertificates repositoryCertificates) {
		Objects.requireNonNull(repositoryCertificates, "repositoryCertificates is required");
		log.info("Updating repository certificates: {}", repositoryCertificates);

		try {
			ArgoSettings settings = settingsProvider.getArgoSettings();
			HttpHeaders httpHeaders = authHeaders(settings);
			ResponseEntity<RepositoryCertificates> create = restClient.exchange(apiPath(settings) + "/certificates",
				HttpMethod.POST,
				new HttpEntity<>(repositoryCertificates, httpHeaders),
				RepositoryCertificates.class);
			RepositoryCertificates createdRepositoryCertificates = create.getBody();
			log.info("Repository certificates updated");
			return createdRepositoryCertificates;

		} catch (RestClientResponseException e) {
			String reason = getReasonMessage(e);
			log.error("Failed to create repository certificates, reason: {}", reason);
			throw new ArgoException("Failed to create repository certificates, reason: " + reason, e);
		}
	}

	protected String apiPath(ArgoSettings settings) {
		return settings.getArgoServer() + "/api/v1";
	}

	protected HttpHeaders authHeaders(ArgoSettings settings) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Authorization", "Bearer " + settings.getArgoToken());
		return httpHeaders;
	}

	private String getReasonMessage(RestClientResponseException e) {
		try {
			return objectMapper.readValue(e.getResponseBodyAsByteArray(), Reason.class).getError();
		} catch (IOException ex) {
			log.warn("Failed to parse response body as a reason for the error: " + e.getMessage());
			return e.getResponseBodyAsString();
		}
	}

	@Data
	private static class Reason {
		private String error;
		private int code;
	}
}
