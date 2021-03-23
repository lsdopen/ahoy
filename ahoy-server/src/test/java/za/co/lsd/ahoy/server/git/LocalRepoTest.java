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

package za.co.lsd.ahoy.server.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.AhoyServerApplication;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = "test")
@Slf4j
public class LocalRepoTest {
	@Autowired
	private LocalRepo localRepo;
	@MockBean
	private SettingsProvider settingsProvider;
	private Git testRemoteRepo;
	@TempDir
	Path temporaryFolder;
	private Path testRepoPath;
	private Path testRemoteRepoPath;

	@BeforeEach
	public void init() throws IOException, GitAPIException {
		testRepoPath = temporaryFolder.resolve("repo");
		testRemoteRepoPath = testRepoPath.resolve("remote.git");
		Files.createDirectories(testRemoteRepoPath);
		testRemoteRepo = Git.init()
			.setDirectory(testRemoteRepoPath.toFile())
			.call();

		when(settingsProvider.getGitSettings()).thenReturn(new GitSettings(testRemoteRepoPath.toUri().toString()));

		localRepo.init();
	}

	@AfterEach
	public void cleanup() throws IOException {
		testRemoteRepo = null;
		localRepo.delete();
	}

	@Test
	public void workingTreePush() throws Exception {
		// given

		// when
		Optional<String> commitHash;
		try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
			Files.writeString(workingTree.getPath().resolve("test.txt"), "test");
			commitHash = workingTree.push("This is a test");
		}
		localRepo.push();

		// then
		assertTrue(commitHash.isPresent(), "We should have a commit");
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals(1, commits.size(), "Incorrect amount of commits");
		assertEquals(commitHash.get(), commits.get(0).getName(), "Remote repo should contain the commit");

		try (LocalRepo.WorkingTree resultWorkingTree = localRepo.requestWorkingTree()) {
			assertTrue(Files.exists(resultWorkingTree.getPath().resolve("test.txt")), "test.txt should exist");
		}
	}

	@Test
	public void workingTreePushDifferentBranch() throws Exception {
		// given
		GitSettings gitSettings = new GitSettings(testRemoteRepoPath.toUri().toString());
		gitSettings.setBranch("main");
		when(settingsProvider.getGitSettings()).thenReturn(gitSettings);

		Files.writeString(testRemoteRepoPath.resolve("README.md"), "This is a test repo");
		testRemoteRepo.add().addFilepattern(".").call();
		testRemoteRepo.commit().setMessage("Added README").call();
		testRemoteRepo.branchCreate().setName("main").call();
		testRemoteRepo.checkout().setName("main").call();

		// when
		Optional<String> commitHash;
		try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
			Files.writeString(workingTree.getPath().resolve("test.txt"), "test");
			commitHash = workingTree.push("This is a test");
		}
		localRepo.push();

		// then
		assertTrue(commitHash.isPresent(), "We should have a commit");
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals(2, commits.size(), "Incorrect amount of commits");
		assertEquals(commitHash.get(), commits.get(0).getName(), "Remote repo should contain the commit");

		try (LocalRepo.WorkingTree resultWorkingTree = localRepo.requestWorkingTree()) {
			assertTrue(Files.exists(resultWorkingTree.getPath().resolve("test.txt")), "test.txt should exist");
		}
	}


	@Test
	public void workingTreePushClean() throws Exception {
		// given

		// when
		Optional<String> commitHash;
		try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
			commitHash = workingTree.push("This is a test");
		}

		// then
		assertFalse(commitHash.isPresent(), "Commit should not have occurred");
	}

	@Test
	public void workingTreePushDeletedFiles() throws Exception {
		// given
		try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
			Path test1 = workingTree.getPath().resolve("test1.txt");
			Path test2 = workingTree.getPath().resolve("test2.txt");
			Files.writeString(test1, "test");
			Files.writeString(test2, "test");
			workingTree.push("This is a test");
			localRepo.push();

			// when
			Files.deleteIfExists(test2);
			workingTree.push("Deleted file push");
		}
		localRepo.push();

		// then
		try (LocalRepo.WorkingTree resultWorkingTree = localRepo.requestWorkingTree()) {
			assertFalse(Files.exists(resultWorkingTree.getPath().resolve("test2.txt")), "test2 should no longer exist");
		}
	}

	@Test
	public void remoteRepoChanged() throws Exception {
		// given
		// an initial working tree
		try (LocalRepo.WorkingTree initialWorkingTree = localRepo.requestWorkingTree()) {
		}

		// a new remote repo
		Path newExpectedRemotePath = testRepoPath.resolve("new-remote.git");
		Files.createDirectories(newExpectedRemotePath);
		Git newRemote = Git.init()
			.setDirectory(newExpectedRemotePath.toFile())
			.call();
		Files.writeString(newExpectedRemotePath.resolve("test.file"), "Test file");
		newRemote.add().addFilepattern("test.file").call();
		RevCommit newRemoteCommit = newRemote.commit().setMessage("New test file").call();

		when(settingsProvider.getGitSettings()).thenReturn(new GitSettings(newExpectedRemotePath.toUri().toString())); // change remote

		// when
		Ref ref;
		try (LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree()) {
			ref = workingTree.headRef();
		}

		// then
		assertEquals(newRemoteCommit.getId(), ref.getObjectId(), "We should have a new remote path");
	}
}
