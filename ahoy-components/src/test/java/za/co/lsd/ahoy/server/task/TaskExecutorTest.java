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

package za.co.lsd.ahoy.server.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.concurrent.ListenableFuture;
import za.co.lsd.ahoy.server.BaseAhoyTest;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static za.co.lsd.ahoy.server.task.TaskProgressEvent.State.*;

@SuppressWarnings({"rawtypes", "unchecked"})
class TaskExecutorTest extends BaseAhoyTest {
	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	private SecuredCollaborator securedCollaborator;
	@Autowired
	private TestTaskProgressListener taskProgressListener;

	private final ProgressMessages progressMessages = new ProgressMessages("test-running", "test-success", "test-failed");

	@BeforeEach
	public void clearProgressEvents() {
		taskProgressListener.clear();
	}

	/**
	 * Tests executing a single synchronous task.
	 */
	@Test
	void executeSyncOne() throws Exception {
		// given
		Task testTask = mock(Task.class);
		doReturn("test").when(testTask).execute();
		taskProgressListener.expectEvents(3);

		// when
		ListenableFuture future = taskExecutor.executeSync(testTask, progressMessages);

		// then
		assertEquals("test", future.get(1000, TimeUnit.MILLISECONDS));
		verify(testTask, timeout(1000).times(1)).execute();
		assertTrue(taskProgressListener.waitForEvents(2000, TimeUnit.MILLISECONDS));
		List<TaskProgressEvent> events = taskProgressListener.getEvents();
		assertEquals(WAITING, events.get(0).getState());
		assertEquals(IN_PROGRESS, events.get(1).getState());
		assertEquals(DONE, events.get(2).getState());
	}

	/**
	 * Tests executing a single synchronous task which throws an Exception.
	 */
	@Test
	void executeSyncOneThrowsException() throws Exception{
		// given
		Task testTask = mock(Task.class);
		doThrow(new RuntimeException("Test failure")).when(testTask).execute();
		taskProgressListener.expectEvents(3);

		// when
		ListenableFuture future = taskExecutor.executeSync(testTask, progressMessages);

		// then
		assertThrows(ExecutionException.class, future::get, "Expected future.get() to throw an ExecutionException");
		verify(testTask, timeout(1000).times(1)).execute();
		assertTrue(taskProgressListener.waitForEvents(2000, TimeUnit.MILLISECONDS));
		List<TaskProgressEvent> events = taskProgressListener.getEvents();
		assertEquals(ERROR, events.get(events.size() - 1).getState());
	}

	/**
	 * Tests executing a single synchronous task without progress.
	 */
	@Test
	void executeSyncOneNoProgress() throws Exception {
		// given
		Task testTask = mock(Task.class);
		doReturn("test").when(testTask).execute();

		// when
		ListenableFuture future = taskExecutor.executeSync(testTask, null);

		// then
		assertEquals("test", future.get(1000, TimeUnit.MILLISECONDS));
		verify(testTask, timeout(1000).times(1)).execute();
		List<TaskProgressEvent> events = taskProgressListener.getEvents();
		assertEquals(0, events.size());
	}

	/**
	 * Tests that executing multiple tasks are executed in sequence when using the synchronous executor.
	 */
	@Test
	void executeSyncMultiple() {
		// given
		Task testTask1 = mock(Task.class);
		Task testTask2 = mock(Task.class);
		Task testTask3 = mock(Task.class);

		// when
		taskExecutor.executeSync(testTask1, null);
		taskExecutor.executeSync(testTask2, null);
		taskExecutor.executeSync(testTask3, null);

		// then
		InOrder inOrder = inOrder(testTask1, testTask2, testTask3);
		inOrder.verify(testTask1, timeout(1000).times(1)).execute();
		inOrder.verify(testTask2, timeout(1000).times(1)).execute();
		inOrder.verify(testTask3, timeout(1000).times(1)).execute();
	}

	/**
	 * Tests that when executing multiple tasks, if an earlier task fails, subsequent tasks are still executed.
	 */
	@Test
	void executeSyncMultipleAfterThrowsException() {
		// given
		Task testTask1 = mock(Task.class);
		Task testTask2 = mock(Task.class);

		doThrow(new RuntimeException("Test failure")).when(testTask1).execute();

		// when
		taskExecutor.executeSync(testTask1, null);
		taskExecutor.executeSync(testTask2, null);

		// then
		verify(testTask1, timeout(1000).times(1)).execute();
		verify(testTask2, timeout(1000).times(1)).execute();
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task executes if the correct auth and role are provided.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeSecuredTask() throws Exception {
		// when
		ListenableFuture<String> future = taskExecutor.executeSync(() -> {
			securedCollaborator.secureMethod();
			return "test";
		}, null);

		// then
		assertEquals("test", future.get(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task DOES NOT execute if no auth and role are provided.
	 */
	@Test
	void executeSecuredTaskWithoutAuth() {
		// when
		ListenableFuture<String> future = taskExecutor.executeSync(() -> {
			securedCollaborator.secureMethod();
			return "test";
		}, null);

		// then
		assertThrows(ExecutionException.class, future::get, "Expected future.get() to throw an ExecutionException because Auth is not in the security context");
	}

	/**
	 * Tests executing a single asynchronous task.
	 */
	@Test
	void executeAsyncOne() {
		// given
		Task testTask = mock(Task.class);

		// when
		taskExecutor.executeAsync(testTask, null);

		// then
		verify(testTask, timeout(1000).times(1)).execute();
	}

	/**
	 * Tests that executing multiple tasks are executed in parallel when using the asynchronous executor.
	 */
	@Test
	void executeAsyncMultiple() {
		// given
		Task testTask = mock(Task.class);

		// when
		taskExecutor.executeAsync(testTask, null);
		taskExecutor.executeAsync(testTask, null);
		taskExecutor.executeAsync(testTask, null);

		// then
		verify(testTask, timeout(2000).times(3)).execute();
	}

	/**
	 * Tests that when executing multiple tasks asynchronously, if an earlier task fails, subsequent tasks are still executed.
	 */
	@Test
	void executeAsyncMultipleAfterThrowsException() {
		// given
		Task testTask1 = mock(Task.class);
		Task testTask2 = mock(Task.class);

		doThrow(new RuntimeException("Test failure")).when(testTask1).execute();
		doReturn("test").when(testTask2).execute();

		// when
		ListenableFuture future1 = taskExecutor.executeAsync(testTask1, null);
		ListenableFuture future2 = taskExecutor.executeAsync(testTask2, null);

		// then
		assertThrows(ExecutionException.class, future1::get, "Expected future.get() to throw an ExecutionException");
		doReturn("test").when(testTask2).execute();
		verify(testTask1, timeout(1000).times(1)).execute();
		verify(testTask2, timeout(1000).times(1)).execute();
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task executes if the correct auth and role are provided.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeAsyncSecuredTask() throws Exception {
		// when
		ListenableFuture<String> future = taskExecutor.executeAsync(() -> {
			securedCollaborator.secureMethod();
			return "test";
		}, null);

		// then
		assertEquals("test", future.get(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	/**
	 * Tests when executing multiple @Secured tasks; with authentication and roles required, that all tasks execute without interfering with each other's auth.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeAsyncMultipleSecuredTasks() throws Exception {
		// when
		ListenableFuture<String> future1 = taskExecutor.executeSync(() -> {
			securedCollaborator.secureMethod();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return "test";
		}, null);

		ListenableFuture<String> future2 = taskExecutor.executeAsync(() -> {
			securedCollaborator.secureMethod();
			return "test";
		}, null);

		// then
		assertEquals("test", future1.get(2000, TimeUnit.MILLISECONDS), "Secured task should have executed");
		assertEquals("test", future2.get(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}
}
