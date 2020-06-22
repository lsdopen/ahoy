/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import za.co.lsd.ahoy.server.AhoyServerApplication;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = "test")
@Slf4j
public class LocalRepoTest {
	@Autowired
	private LocalRepo localRepo;
	@MockBean
	private SettingsProvider settingsProvider;
	private Git testRemoteRepo;
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder(Paths.get("./target").toFile());
	private Path testRepoPath;

	@Before
	public void init() throws IOException, GitAPIException {
		testRepoPath = temporaryFolder.newFolder("repo").toPath();
		Path testRemoteRepoPath = testRepoPath.resolve("remote.git");
		Files.createDirectories(testRemoteRepoPath);
		testRemoteRepo = Git.init()
			.setDirectory(testRemoteRepoPath.toFile())
			.setBare(true)
			.call();

		when(settingsProvider.getGitSettings()).thenReturn(new GitSettings(testRemoteRepoPath.toUri().toString()));

		localRepo.init();
	}

	@After
	public void cleanup() throws IOException {
		testRemoteRepo = null;
		localRepo.delete();
	}

	@Test
	public void workingTreePush() throws Exception {
		// given

		// when
		LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree();
		Files.writeString(workingTree.getPath().resolve("test.txt"), "test");
		Optional<String> commitHash = workingTree.push("This is a test");
		workingTree.delete();
		localRepo.push();

		// then
		assertTrue("We should have a commit", commitHash.isPresent());
		Iterable<RevCommit> revCommits = testRemoteRepo.log().call();
		List<RevCommit> commits = StreamSupport.stream(revCommits.spliterator(), false).collect(Collectors.toList());
		assertEquals("Incorrect amount of commits", 1, commits.size());
		assertEquals("Remote repo should contain the commit", commitHash.get(), commits.get(0).getName());

		LocalRepo.WorkingTree resultWorkingTree = localRepo.requestWorkingTree();
		assertTrue("test.txt should exist", Files.exists(resultWorkingTree.getPath().resolve("test.txt")));
		resultWorkingTree.delete();
	}

	@Test
	public void workingTreePushClean() throws Exception {
		// given

		// when
		LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree();
		Optional<String> commitHash = workingTree.push("This is a test");
		workingTree.delete();

		// then
		assertFalse("Commit should not have occurred", commitHash.isPresent());
	}

	@Test
	public void workingTreePushDeletedFiles() throws Exception {
		// given
		LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree();
		Path test1 = workingTree.getPath().resolve("test1.txt");
		Path test2 = workingTree.getPath().resolve("test2.txt");
		Files.writeString(test1, "test");
		Files.writeString(test2, "test");
		workingTree.push("This is a test");
		localRepo.push();

		// when
		Files.deleteIfExists(test2);
		workingTree.push("Deleted file push");
		workingTree.delete();
		localRepo.push();

		// then
		LocalRepo.WorkingTree resultWorkingTree = localRepo.requestWorkingTree();
		assertFalse("test2 should no longer exist", Files.exists(resultWorkingTree.getPath().resolve("test2.txt")));
		resultWorkingTree.delete();
	}

	@Test
	public void remoteRepoChanged() throws Exception {
		// given
		// an initial working tree
		LocalRepo.WorkingTree initialWorkingTree = localRepo.requestWorkingTree();
		initialWorkingTree.delete();

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
		LocalRepo.WorkingTree workingTree = localRepo.requestWorkingTree();
		Ref ref = workingTree.headRef();
		workingTree.delete();

		// then
		assertEquals("We should have a new remote path", newRemoteCommit.getId(), ref.getObjectId());
	}
}
