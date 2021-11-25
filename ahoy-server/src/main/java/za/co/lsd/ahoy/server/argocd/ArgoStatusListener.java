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

package za.co.lsd.ahoy.server.argocd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
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

@Component
@Slf4j
public class ArgoStatusListener {
	private final SettingsProvider settingsProvider;
	private final WebClient webClient;
	private final ArgoClient argoClient;
	private final ReleaseService releaseService;
	private Disposable eventStreamSubscription;

	public ArgoStatusListener(SettingsProvider settingsProvider, WebClient webClient, ArgoClient argoClient, ReleaseService releaseService) {
		this.settingsProvider = settingsProvider;
		this.webClient = webClient;
		this.argoClient = argoClient;
		this.releaseService = releaseService;
	}

	private void connect() {
		try {
			ArgoSettings argoSettings = settingsProvider.getArgoSettings();
			if (argoSettings.configured()) {
				Flux<ArgoApplicationWatchEvent> eventStream = webClient.get()
					.uri(argoClient.apiPath(argoSettings) + "/stream/applications")
					.headers(httpHeaders -> httpHeaders.putAll(argoClient.authHeaders(argoSettings)))
					.retrieve()
					.bodyToFlux(ArgoApplicationWatchEvent.class);

				this.eventStreamSubscription = eventStream.retryWhen(
					Retry.backoff(5, Duration.ofSeconds(3)).maxBackoff(Duration.ofSeconds(10))
						.filter(error -> {
							log.warn("Failed to subscribe to argocd status stream, retrying... Error: {}", error.getMessage());
							return true;
						}))
					.subscribe(this::event,
						error -> {
							log.error("Failed to subscribe to argocd status stream: " + error.getMessage(), error);
							close();
						},
						() -> {
							log.debug("Subscription to argocd status stream completed");
							close();
						},
						subscription -> {
							subscription.request(Long.MAX_VALUE);
							log.info("Subscribed to argocd status stream");
						});

			} else {
				log.warn("Unable to subscribe to argocd status stream, argocd not yet configured");
			}
		} catch (Exception e) {
			log.error("Failed to subscribe to argocd status stream", e);
		}
	}

	@Scheduled(initialDelay = 10000, fixedDelay = 3000)
	@RunAsRole(Role.admin)
	public void checkSubscription() {
		if (eventStreamSubscription == null) {
			connect();
		}
	}

	@PreDestroy
	public void close() {
		if (eventStreamSubscription != null && !eventStreamSubscription.isDisposed()) {
			eventStreamSubscription.dispose();
		}
		eventStreamSubscription = null;
	}

	private void event(ArgoApplicationWatchEvent event) {
		log.trace("Argo application event occurred: {}", event);

		ArgoApplication application = event.getResult().getApplication();
		if (application.getStatus().hasResources()) {
			releaseService.updateStatus(application);
		}
	}
}
