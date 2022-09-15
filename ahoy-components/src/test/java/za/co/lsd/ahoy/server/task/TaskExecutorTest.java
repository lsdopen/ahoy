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

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import za.co.lsd.ahoy.server.BaseAhoyTest;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.Scope;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static za.co.lsd.ahoy.server.task.TaskProgressEvent.State.*;

class TaskExecutorTest extends BaseAhoyTest {
	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	private SecuredTask securedTask;
	@Autowired
	private TestTaskProgressListener taskProgressListener;

	/**
	 * Tests executing a single synchronous task.
	 */
	@Test
	void executeSyncOne() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-task-id");
		TestTask mockTask = mockTask();

		// when
		taskExecutor.executeSync(mockTask, context);

		// then
		verify(mockTask, timeout(1000).times(1)).execute(same(context));
		List<TaskProgressEvent> events = taskProgressListener.getEvents(context.getId());
		assertEquals(3, events.size());
		assertEquals(WAITING, events.get(0).getState());
		assertEquals(IN_PROGRESS, events.get(1).getState());
		assertEquals(DONE, events.get(2).getState());
	}

	/**
	 * Tests executing a single synchronous task without progress.
	 */
	@Test
	void executeSyncOneNoProgress() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-task-id", false);
		TestTask mockTask = mockTask();

		// when
		taskExecutor.executeSync(mockTask, context);

		// then
		verify(mockTask, timeout(1000).times(1)).execute(same(context));
		List<TaskProgressEvent> events = taskProgressListener.getEvents(context.getId());
		assertEquals(0, events.size());
	}

	/**
	 * Tests that executing multiple tasks are executed in sequence when using the synchronous executor.
	 */
	@Test
	void executeSyncMultiple() throws Exception {
		// given
		TestTaskContext task1Context = new TestTaskContext("my-task-id-1");
		TestTaskContext task2Context = new TestTaskContext("my-task-id-2");
		TestTaskContext task3Context = new TestTaskContext("my-task-id-3");
		TestTask mockTask = mockTask();

		// when
		taskExecutor.executeSync(mockTask, task1Context);
		taskExecutor.executeSync(mockTask, task2Context);
		taskExecutor.executeSync(mockTask, task3Context);

		// then
		InOrder inOrder = inOrder(mockTask);
		inOrder.verify(mockTask, timeout(1000).times(1)).execute(same(task1Context));
		inOrder.verify(mockTask, timeout(1000).times(1)).execute(same(task2Context));
		inOrder.verify(mockTask, timeout(1000).times(1)).execute(same(task3Context));
	}

	/**
	 * Tests that when executing multiple tasks, if an earlier task fails, subsequent tasks are still executed.
	 */
	@Test
	void executeSyncMultipleAfterThrowsException() throws Exception {
		// given
		TestTaskContext task1Context = new TestTaskContext("my-task-id-1");
		TestTaskContext task2Context = new TestTaskContext("my-task-id-2");
		TestTask mockTask1 = mockTask();
		TestTask mockTask2 = mockTask();
		doThrow(new RuntimeException("Test failure")).when(mockTask1).execute(any());

		// when
		taskExecutor.executeSync(mockTask1, task1Context);
		taskExecutor.executeSync(mockTask2, task2Context);

		// then
		verify(mockTask1, timeout(1000).times(1)).execute(same(task1Context));
		verify(mockTask2, timeout(1000).times(1)).execute(same(task2Context));
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task executes if the correct auth and role are provided.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeSecuredTask() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(1));

		// when
		taskExecutor.executeSync(securedTask, context);

		// then
		assertTrue(securedTask.awaitExecution(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task DOES NOT execute if no auth and role are provided.
	 */
	@Test
	void executeSecuredTaskWithoutAuth() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(1));

		// when
		taskExecutor.executeSync(securedTask, context);

		// then
		assertFalse(securedTask.awaitExecution(1000, TimeUnit.MILLISECONDS), "Secured task should NOT have executed");
	}

	/**
	 * Tests executing a single asynchronous task.
	 */
	@Test
	void executeAsyncOne() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-task-id");
		TestTask mockTask = mockTask();

		// when
		taskExecutor.executeAsync(mockTask, context);

		// then
		verify(mockTask, timeout(1000).times(1)).execute(same(context));
	}

	/**
	 * Tests that executing multiple tasks are executed in parallel when using the asynchronous executor.
	 */
	@Test
	void executeAsyncMultiple() throws Exception {
		// given
		TestTaskContext task1Context = new TestTaskContext("my-task-id-1");
		TestTaskContext task2Context = new TestTaskContext("my-task-id-2");
		TestTaskContext task3Context = new TestTaskContext("my-task-id-3");
		TestTask mockTask = mockTask(1000);

		// when
		taskExecutor.executeAsync(mockTask, task1Context);
		taskExecutor.executeAsync(mockTask, task2Context);
		taskExecutor.executeAsync(mockTask, task3Context);

		// then
		verify(mockTask, timeout(2000).times(3)).execute(any());
	}

	/**
	 * Tests that when executing multiple tasks asynchronously, if an earlier task fails, subsequent tasks are still executed.
	 */
	@Test
	void executeAsyncMultipleAfterThrowsException() throws Exception {
		// given
		TestTaskContext task1Context = new TestTaskContext("my-task-id-1");
		TestTaskContext task2Context = new TestTaskContext("my-task-id-2");
		TestTask mockTask1 = mockTask();
		TestTask mockTask2 = mockTask();
		doThrow(new RuntimeException("Test failure")).when(mockTask1).execute(any());

		// when
		taskExecutor.executeAsync(mockTask1, task1Context);
		taskExecutor.executeAsync(mockTask2, task2Context);

		// then
		verify(mockTask1, timeout(1000).times(1)).execute(same(task1Context));
		verify(mockTask2, timeout(1000).times(1)).execute(same(task2Context));
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task executes if the correct auth and role are provided.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeAsyncSecuredTask() throws Exception {
		// given
		TestTaskContext context = new TestTaskContext("my-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(1));

		// when
		taskExecutor.executeAsync(securedTask, context);

		// then
		assertTrue(securedTask.awaitExecution(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	/**
	 * Tests when executing multiple @Secured tasks; with authentication and roles required, that all tasks execute without interfering with each other's auth.
	 */
	@Test
	@WithMockUser(authorities = {Scope.ahoy, Role.admin})
	void executeAsyncMultipleSecuredTasks() throws Exception {
		// given
		TestTaskContext syncContext = new TestTaskContext("my-sync-secure-task-id");
		syncContext.setSleep(1000);
		TestTaskContext asyncContext = new TestTaskContext("my-async-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(2));

		// when
		taskExecutor.executeSync(securedTask, syncContext);
		taskExecutor.executeAsync(securedTask, asyncContext);

		// then
		assertTrue(securedTask.awaitExecution(2000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	private TestTask mockTask() {
		TestTask mockTask = mock(TestTask.class);
		when(mockTask.getName()).thenReturn("mock-task");
		return mockTask;
	}

	private TestTask mockTask(long sleep) {
		TestTask mockTask = mock(TestTask.class);
		when(mockTask.getName()).thenReturn("mock-task");
		doAnswer((invocation) -> {
			Thread.sleep(sleep);
			return null;
		}).when(mockTask).execute(any(TestTaskContext.class));
		return mockTask;
	}
}
