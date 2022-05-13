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
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.AhoyServerProperties;
import za.co.lsd.ahoy.server.settings.SettingsProvider;
import za.co.lsd.ahoy.server.util.FileUtils;

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
	private Git gitRepo;

	public LocalRepo(AhoyServerProperties serverProperties, SettingsProvider settingsProvider) {
		repoPath = Paths.get(serverProperties.getRepoPath());
		this.settingsProvider = settingsProvider;
		localRepoPath = repoPath.resolve(LOCAL_REPO_NAME);
	}

	@PostConstruct
	public void init() {
		try {
			if (gitRepo == null) {
				if (!Files.exists(localRepoPath)) {
					Files.createDirectories(localRepoPath);

					log.info("Local repo not found, initialising repo: {}", localRepoPath);
					gitRepo = Git.init()
						.setDirectory(localRepoPath.toFile())
						.setBare(true)
						.call();

				} else {
					log.info("Local repo found, opening: {}", localRepoPath);
					gitRepo = Git.open(localRepoPath.toFile());
				}

				saveRepoConfig();
			}

		} catch (Exception e) {
			try {
				FileUtils.deleteRecursively(localRepoPath);
			} catch (IOException ex) {
				log.warn("Failed to cleanup local repo directory");
			}
			log.error("Failed to init/open local repo", e);
			throw new LocalRepoException("Failed to init/open local repo", e);
		}
	}

	private void saveRepoConfig() throws IOException {
		StoredConfig config = gitRepo.getRepository().getConfig();
		// TODO disable sslVerify until an administrator can supply a custom CA certificate to be trusted
		config.setBoolean("http", null, "sslVerify", false);
		config.save();
	}

	public WorkingTree requestWorkingTree() {
		try {
			GitSettings gitSettings = settingsProvider.getGitSettings();

			log.info("Requesting new working tree");
			fetch();
			WorkingTree workingTree = new WorkingTree(gitSettings);
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
			FetchResult fetchResult = configureCredentials(gitSettings, gitRepo.fetch()
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
			Iterable<PushResult> pushResults = configureCredentials(gitSettings, gitRepo.push()
				.add(gitSettings.getBranch())
				.setRemote(gitSettings.getRemoteRepoUri()))
				.call();
			pushResults.forEach(pushResult -> log.info("Result: {}", pushResult.getRemoteUpdates()));
			pushResults.forEach(LocalRepo::pushResultThrowForFailures);
		} catch (Exception e) {
			throw new LocalRepoException("Failed to push local repo", e);
		}
	}

	public void testConnection(GitSettings testGitSettings) {
		try {
			log.info("Testing connection to git repo remote: {}", testGitSettings.getRemoteRepoUri());
			FetchResult fetchResult = configureCredentials(testGitSettings, gitRepo.fetch()
				.setRemote(testGitSettings.getRemoteRepoUri())
				.setForceUpdate(true)
				.setRefSpecs("refs/heads/*:refs/heads/*"))
				.call();
			resultThrowForFailures(fetchResult);
			log.debug("Connection fetch result: {}", fetchResult.getTrackingRefUpdates());
		} catch (Exception e) {
			throw new LocalRepoException("Failed to connect to remote repo", e);
		}
	}

	public void delete() {
		try {
			if (gitRepo != null) {
				log.info("Closing local repo: {}", localRepoPath);
				gitRepo.close();
				log.info("Deleting local repo: {}", localRepoPath);
				FileUtils.deleteRecursively(localRepoPath);
				gitRepo = null;
			}
		} catch (Exception e) {
			log.error("Failed to delete local repo", e);
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
				default:
					break;
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
				default:
					break;
			}
		});
	}

	public class WorkingTree implements AutoCloseable {
		private final Path workingTreePath;
		private final Git gitWorkingTree;

		public WorkingTree(GitSettings gitSettings) {
			try {
				workingTreePath = Files.createTempDirectory(repoPath, "working");

				gitWorkingTree = Git.cloneRepository()
					.setURI(localRepoPath.toUri().toString())
					.setDirectory(workingTreePath.toFile())
					.setBranch(gitSettings.getBranch())
					.call();
			} catch (Exception e) {
				throw new LocalRepoException("Failed to create working tree", e);
			}
		}

		public Path getPath() {
			return workingTreePath;
		}

		public Optional<String> push(String message) {
			try {
				log.info("Pushing working tree with message: {}", message);

				Status status = gitWorkingTree.status().call();
				if (status.isClean()) {
					log.info("Working tree is clean, won't commit or push");
					return Optional.empty();

				} else {
					gitWorkingTree.add()
						.addFilepattern(".")
						.call();

					Set<String> missing = status.getMissing();
					if (!missing.isEmpty()) {
						RmCommand rm = gitWorkingTree.rm();
						missing.forEach(rm::addFilepattern);
						rm.call();
					}

					RevCommit commit = gitWorkingTree.commit()
						.setMessage(message)
						.call();

					gitWorkingTree.push().call();
					log.info("Pushed working tree: {}", commit.getName());
					return Optional.of(commit.getName());
				}
			} catch (Exception e) {
				throw new LocalRepoException("Failed to push working tree", e);
			}
		}

		public Ref headRef() {
			try {
				return gitWorkingTree.getRepository().exactRef(Constants.HEAD);
			} catch (Exception e) {
				throw new LocalRepoException("Failed to get head ref", e);
			}
		}

		@Override
		public void close() throws Exception {
			try {
				if (Files.exists(workingTreePath)) {
					log.info("Closing working tree: {}", workingTreePath);
					gitWorkingTree.close();
					log.info("Deleting working tree: {}", workingTreePath);
					FileUtils.deleteRecursively(workingTreePath);
				}
			} catch (Exception e) {
				log.error("Failed to delete working tree", e);
				throw new LocalRepoException("Failed to delete working tree", e);
			}
		}
	}
}
