package za.co.lsd.ahoy.server.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import za.co.lsd.ahoy.server.AhoyServerProperties;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class LocalRepo {
	private static final String LOCAL_REPO_NAME = "releases.git";
	private final SettingsProvider settingsProvider;
	private final Path repoPath;
	private final Path localRepoPath;
	private Git localRepo;

	public LocalRepo(AhoyServerProperties serverProperties, SettingsProvider settingsProvider) {
		repoPath = Paths.get(serverProperties.getRepoPath());
		this.settingsProvider = settingsProvider;
		localRepoPath = repoPath.resolve(LOCAL_REPO_NAME);
	}

	@PostConstruct
	public void init() {
		try {
			if (localRepo == null) {
				if (!Files.exists(localRepoPath)) {
					Files.createDirectories(localRepoPath);

					log.info("Local repo not found, initialising repo: {}", localRepoPath);
					localRepo = Git.init()
						.setDirectory(localRepoPath.toFile())
						.setBare(true)
						.call();

				} else {
					log.info("Local repo found, opening: {}", localRepoPath);
					localRepo = Git.open(localRepoPath.toFile());
				}
			}

		} catch (Exception e) {
			try {
				FileSystemUtils.deleteRecursively(localRepoPath);
			} catch (IOException ex) {
				log.warn("Failed to cleanup local repo directory");
			}
			log.error("Failed to init/open local repo", e);
			throw new LocalRepoException("Failed to init/open local repo", e);
		}
	}

	public WorkingTree requestWorkingTree() {
		try {
			log.info("Requesting new working tree");
			fetch();
			WorkingTree workingTree = new WorkingTree();
			log.info("Working tree created: {}, head ref: {}", workingTree.getPath(), workingTree.headRef());
			return workingTree;
		} catch (Exception e) {
			throw new LocalRepoException("Failed to request a new working tree", e);
		}
	}

	public void fetch() {
		try {
			GitSettings gitSettings = settingsProvider.getGitSettings();

			log.info("Fetching local repo from remote: {}", gitSettings.getRemoteRepoUri());
			FetchResult fetchResult = configureCredentials(gitSettings, localRepo.fetch()
				.setRemote(gitSettings.getRemoteRepoUri())
				.setForceUpdate(true)
				.setRefSpecs("refs/heads/*:refs/heads/*"))
				.call();
			resultThrowForFailures(fetchResult);
			log.debug("Fetch result: {}", fetchResult.getTrackingRefUpdates());
		} catch (Exception e) {
			throw new LocalRepoException("Failed to fetch from remote repo", e);
		}
	}

	public void push() {
		try {
			GitSettings gitSettings = settingsProvider.getGitSettings();

			log.info("Pushing local repo to remote: {}", gitSettings.getRemoteRepoUri());
			Iterable<PushResult> pushResults = configureCredentials(gitSettings, localRepo.push()
				.setRemote(gitSettings.getRemoteRepoUri()))
				.call();
			pushResults.forEach(pushResult -> log.info("Result: {}", pushResult.getRemoteUpdates()));
			pushResults.forEach(LocalRepo::pushResultThrowForFailures);
		} catch (Exception e) {
			throw new LocalRepoException("Failed to push local repo", e);
		}
	}

	public void delete() {
		try {
			if (localRepo != null) {
				log.info("Deleting local repo: {}", localRepoPath);
				FileSystemUtils.deleteRecursively(localRepoPath);
				localRepo = null;
			}
		} catch (Exception e) {
			throw new LocalRepoException("Failed to delete local repo", e);
		}
	}

	private <C extends GitCommand, T> TransportCommand<C, T> configureCredentials(GitSettings gitSettings, TransportCommand<C, T> transportCommand) {
		if (GitSettings.Credentials.HTTPS.equals(gitSettings.getCredentials())) {
			transportCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitSettings.getHttpsUsername(), gitSettings.getHttpsPassword()));

		} else if (GitSettings.Credentials.SSH.equals(gitSettings.getCredentials())) {
			SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
				@Override
				protected void configure(OpenSshConfig.Host host, Session session) {
					// do nothing
				}

				@Override
				protected JSch createDefaultJSch(FS fs) throws JSchException {
					JSch defaultJSch = super.createDefaultJSch(fs);
					String pvtKey = gitSettings.getPrivateKey();
					defaultJSch.addIdentity("git", pvtKey.getBytes(StandardCharsets.US_ASCII), null, null);
					String knownHosts = gitSettings.getSshKnownHosts();
					defaultJSch.setKnownHosts(IOUtils.toInputStream(knownHosts, StandardCharsets.US_ASCII));
					return defaultJSch;
				}
			};
			transportCommand.setTransportConfigCallback(transport -> {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(sshSessionFactory);
			});
		}
		return transportCommand;
	}

	private static void resultThrowForFailures(OperationResult result) throws LocalRepoException {
		result.getTrackingRefUpdates().forEach(refUpdate -> {
			RefUpdate.Result refUpdateResult = refUpdate.getResult();
			switch (refUpdateResult) {
				case IO_FAILURE:
				case LOCK_FAILURE:
				case REJECTED:
				case REJECTED_CURRENT_BRANCH:
				case REJECTED_MISSING_OBJECT:
				case REJECTED_OTHER_REASON:
					throw new LocalRepoException("Ref update for " + refUpdate + ": " + refUpdateResult);
			}
		});
	}

	private static void pushResultThrowForFailures(PushResult result) throws LocalRepoException {
		resultThrowForFailures(result);
		result.getRemoteUpdates().forEach(update -> {
			RemoteRefUpdate.Status updateStatus = update.getStatus();
			switch (updateStatus) {
				case NON_EXISTING:
				case REJECTED_NODELETE:
				case REJECTED_NONFASTFORWARD:
				case REJECTED_REMOTE_CHANGED:
					throw new LocalRepoException("Ref update for " + update + ": " + updateStatus);
			}
		});
	}

	public class WorkingTree {
		private final Path workingTreePath;
		private final Git workingTree;
		private boolean valid = true;

		public WorkingTree() {
			try {
				workingTreePath = Files.createTempDirectory(repoPath, "working");

				workingTree = Git.cloneRepository()
					.setURI(localRepoPath.toUri().toString())
					.setDirectory(workingTreePath.toFile())
					.call();
			} catch (Exception e) {
				throw new LocalRepoException("Failed to create working tree", e);
			}
		}

		public Path getPath() {
			checkValid();

			return workingTreePath;
		}

		public Optional<String> push(String message) {
			checkValid();

			try {
				log.info("Pushing working tree with message: {}", message);

				Status status = workingTree.status().call();
				if (status.isClean()) {
					log.info("Working tree is clean, won't commit or push");
					return Optional.empty();

				} else {
					workingTree.add()
						.addFilepattern(".")
						.call();

					Set<String> missing = status.getMissing();
					if (!missing.isEmpty()) {
						RmCommand rm = workingTree.rm();
						missing.forEach(rm::addFilepattern);
						rm.call();
					}

					RevCommit commit = workingTree.commit()
						.setMessage(message)
						.call();

					workingTree.push().call();
					log.info("Pushed working tree: {}", commit.getName());
					return Optional.of(commit.getName());
				}
			} catch (Exception e) {
				throw new LocalRepoException("Failed to push working tree", e);
			}
		}

		public Ref headRef() {
			try {
				return workingTree.getRepository().exactRef(Constants.HEAD);
			} catch (Exception e) {
				throw new LocalRepoException("Failed to get head ref", e);
			}
		}

		public void delete() {
			try {
				if (valid) {
					log.info("Deleting working tree: {}", workingTreePath);
					FileSystemUtils.deleteRecursively(workingTreePath);
					valid = false;
				}
			} catch (Exception e) {
				throw new LocalRepoException("Failed to delete working tree", e);
			}
		}

		private void checkValid() {
			if (!valid)
				throw new LocalRepoException("Working tree is invalid, it was probably deleted...");
		}
	}
}
