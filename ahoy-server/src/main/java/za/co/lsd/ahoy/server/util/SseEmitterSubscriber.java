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

package za.co.lsd.ahoy.server.util;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class SseEmitterSubscriber<T> implements Subscriber<T> {
	private Subscription subscription;
	private final SseEmitter emitter;

	public SseEmitterSubscriber(SseEmitter emitter) {
		this.emitter = Objects.requireNonNull(emitter);
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;
		subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onNext(T t) {
		try {
			emitter.send(t);
		} catch (IOException e) {
			log.warn("Failed to send next object: " + e.getMessage());
			subscription.cancel();
			emitter.completeWithError(e);
		}
	}

	@Override
	public void onError(Throwable t) {
		emitter.completeWithError(t);
	}

	@Override
	public void onComplete() {
		emitter.complete();
	}
}
