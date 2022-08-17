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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskExecutorTest extends BaseAhoyTest {
	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	private SecuredTask securedTask;

	/**
	 * Tests executing a single task.
	 */
	@Test
	void executeOne() throws Exception {
		// given
		TaskContext context = new TestTaskContext("my-task-id");
		Task mockTask = mock(Task.class);

		// when
		taskExecutor.execute(mockTask, context);

		// then
		verify(mockTask, timeout(1000).times(1)).execute(same(context));
	}

	/**
	 * Tests that executing multiple tasks are executed in sequence when using the synchronous executor.
	 */
	@Test
	void executeMultipleSynchronously() throws Exception {
		// given
		TaskContext task1Context = new TestTaskContext("my-task-id-1");
		TaskContext task2Context = new TestTaskContext("my-task-id-2");
		TaskContext task3Context = new TestTaskContext("my-task-id-3");
		Task mockTask = mock(Task.class);

		// when
		taskExecutor.execute(mockTask, task1Context);
		taskExecutor.execute(mockTask, task2Context);
		taskExecutor.execute(mockTask, task3Context);

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
	void executeMultipleAfterThrowsException() throws Exception {
		// given
		TaskContext task1Context = new TestTaskContext("my-task-id-1");
		TaskContext task2Context = new TestTaskContext("my-task-id-2");
		Task mockTask1 = mock(Task.class);
		Task mockTask2 = mock(Task.class);
		doThrow(new RuntimeException("Test failure")).when(mockTask1).execute(any());

		// when
		taskExecutor.execute(mockTask1, task1Context);
		taskExecutor.execute(mockTask2, task2Context);

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
		TaskContext context = new TestTaskContext("my-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(1));

		// when
		taskExecutor.execute(securedTask, context);

		// then
		assertTrue(securedTask.awaitExecution(1000, TimeUnit.MILLISECONDS), "Secured task should have executed");
	}

	/**
	 * Tests when executing a @Secured task; one with authentication and role required, that the task DOES NOT execute if no auth and role are provided.
	 */
	@Test
	void executeSecuredTaskWithoutAuth() throws Exception {
		// given
		TaskContext context = new TestTaskContext("my-secure-task-id");
		securedTask.setExecutedLatch(new CountDownLatch(1));

		// when
		taskExecutor.execute(securedTask, context);

		// then
		assertFalse(securedTask.awaitExecution(1000, TimeUnit.MILLISECONDS), "Secured task should NOT have executed");
	}

	public static class TestTaskContext extends TaskContext {

		public TestTaskContext(String id) {
			super(id);
		}
	}
}
