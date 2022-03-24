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

package za.co.lsd.ahoy.server.argocd;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import za.co.lsd.ahoy.server.ReleaseService;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplication;
import za.co.lsd.ahoy.server.argocd.model.ArgoApplicationWatchEvent;
import za.co.lsd.ahoy.server.security.Role;
import za.co.lsd.ahoy.server.security.RunAsRole;
import za.co.lsd.ahoy.server.settings.SettingsProvider;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ArgoStatusListener implements Subscriber<ArgoApplicationWatchEvent> {
	private final SettingsProvider settingsProvider;
	private final WebClient webClient;
	private final ArgoClient argoClient;
	private final ReleaseService releaseService;
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private Subscription eventStreamSubscription;
	private ApplicationEventPublisher applicationEventPublisher;

	public ArgoStatusListener(SettingsProvider settingsProvider, WebClient webClient, ArgoClient argoClient, ReleaseService releaseService) {
		this.settingsProvider = settingsProvider;
		this.webClient = webClient;
		this.argoClient = argoClient;
		this.releaseService = releaseService;
	}

	@Autowired
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public boolean isConnected() {
		return connected.get();
	}

	private void connect() {
		try {
			if (settingsProvider.argoSettingsExists()) {
				ArgoSettings argoSettings = settingsProvider.getArgoSettings();
				if (argoSettings.configured()) {
					Flux<ArgoApplicationWatchEvent> eventStream = webClient.get()
						.uri(argoClient.apiPath(argoSettings) + "/stream/applications")
						.headers(httpHeaders -> httpHeaders.putAll(argoClient.authHeaders(argoSettings)))
						.retrieve()
						.bodyToFlux(ArgoApplicationWatchEvent.class)
						.retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(3))
							.filter(error -> {
								log.warn("Failed to subscribe to ArgoCD status stream, retrying... Error: {}", error.getMessage());
								connected(false);
								return true;
							}));

					eventStream.subscribe(this);

				} else {
					log.warn("Unable to subscribe to ArgoCD status stream, ArgoCD not yet configured");
				}
			}
		} catch (Exception e) {
			log.error("Failed to subscribe to ArgoCD status stream", e);
		}
	}

	@Scheduled(initialDelay = 10000, fixedDelay = 3000)
	@RunAsRole(Role.admin)
	public void checkSubscription() {
		if (eventStreamSubscription == null) {
			connect();
		}
	}

	@Scheduled(initialDelay = 15000, fixedDelay = 5000)
	@RunAsRole(Role.admin)
	public void checkConnected() {
		if (!connected.get()) {
			if (settingsProvider.argoSettingsExists()) {
				ArgoSettings argoSettings = settingsProvider.getArgoSettings();
				if (argoSettings.configured() && argoClient.silentTestConnection(argoSettings))
					connected(true);
			}
		}
	}

	@PreDestroy
	public void close() {
		if (eventStreamSubscription != null) {
			eventStreamSubscription.cancel();
		}
		eventStreamSubscription = null;
	}

	@EventListener
	public void onArgoSettingsChanged(ArgoSettingsChangedEvent event) {
		log.info("ArgoCD settings changed, closing current connection and re-connecting...");
		close();
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		eventStreamSubscription = subscription;
		subscription.request(Long.MAX_VALUE);
		log.info("Subscribed to ArgoCD status stream");
	}

	@Override
	public void onNext(ArgoApplicationWatchEvent argoApplicationWatchEvent) {
		log.trace("ArgoCD application event occurred: {}", argoApplicationWatchEvent);

		ArgoApplication application = argoApplicationWatchEvent.getResult().getApplication();
		if (application.getStatus().hasResources()) {
			releaseService.updateStatus(application);
		}
	}

	@Override
	public void onError(Throwable error) {
		log.error("Failed to subscribe to ArgoCD status stream: " + error.getMessage(), error);
		connected(false);
		close();
	}

	@Override
	public void onComplete() {
		log.debug("Subscription to ArgoCD status stream completed");
	}

	private void connected(boolean connectedFlag) {
		boolean prevConnected = connected.getAndSet(connectedFlag);
		if (!prevConnected && connectedFlag) {
			publishConnectionEvent(true);

		} else if (prevConnected && !connectedFlag) {
			publishConnectionEvent(false);
		}
	}

	private void publishConnectionEvent(boolean connectedFlag) {
		applicationEventPublisher.publishEvent(new ArgoConnectionEvent(this, connectedFlag));
	}
}
