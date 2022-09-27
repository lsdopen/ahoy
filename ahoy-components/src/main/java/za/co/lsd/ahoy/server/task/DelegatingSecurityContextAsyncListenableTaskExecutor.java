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

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

public class DelegatingSecurityContextAsyncListenableTaskExecutor extends DelegatingSecurityContextAsyncTaskExecutor implements AsyncListenableTaskExecutor {

	public DelegatingSecurityContextAsyncListenableTaskExecutor(AsyncListenableTaskExecutor delegateAsyncTaskExecutor, SecurityContext securityContext) {
		super(delegateAsyncTaskExecutor, securityContext);
	}

	public DelegatingSecurityContextAsyncListenableTaskExecutor(AsyncListenableTaskExecutor delegateAsyncTaskExecutor) {
		super(delegateAsyncTaskExecutor);
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		return getDelegate().submitListenable(wrap(task));
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		return getDelegate().submitListenable(wrap(task));
	}

	private AsyncListenableTaskExecutor getDelegate() {
		return (AsyncListenableTaskExecutor) getDelegateExecutor();
	}
}
