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

import {EventEmitter, Inject, Injectable, isDevMode, OnDestroy} from '@angular/core';
import {AuthService} from '../util/auth.service';
import {LoggerService} from '../util/logger.service';
import {TaskEvent} from './task-events';
import {Client} from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class TaskEventsService implements OnDestroy {
  private readonly serverUrl;
  private client: Client;
  public taskEvents = new EventEmitter<TaskEvent>();

  constructor(private authService: AuthService,
              private log: LoggerService,
              @Inject('environment') environment) {

    if (isDevMode()) {
      this.serverUrl = 'ws://localhost:8080/ws';

    } else {
      const protocol = window.location.protocol === 'http:' ? 'ws' : 'wss';
      this.serverUrl = `${protocol}://${window.location.host}/ws`;
    }

    if (environment.taskEventsWebsocketEnabled) {
      this.connect();

    } else {
      this.log.warn('Task event websocket connection disabled');
    }
  }

  private connect() {
    this.client = new Client({
      brokerURL: this.serverUrl,
      connectHeaders: {'X-Authorization': 'Bearer ' + this.authService.accessToken()},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.client.onConnect = frame => {
      this.log.debug('Websocket connection established:', frame);
      this.client.subscribe('/events', (message => this.receivedAppMessage(message)));
      this.client.subscribe('/user/events', (message) => this.receivedUserMessage(message));
    };

    this.client.onWebSocketError = frame => {
      this.log.warn('Websocket connection failed: ', frame);
    };

    this.client.onStompError = frame => {
      this.log.warn('Broker reported error: ', frame);
    };

    this.client.activate();
  }

  ngOnDestroy(): void {
    if (this.client) {
      this.client.deactivate()
        .then(() => this.log.debug('Websocket connection deactivated'));
    }
  }

  private receivedAppMessage(message) {
    if (message.body) {
      this.log.debug('Received websocket app message: ', message);
      const taskEvent = JSON.parse(message.body) as TaskEvent;
      this.taskEvents.emit(taskEvent);
    }
  }

  private receivedUserMessage(message) {
    if (message.body) {
      this.log.debug('Received websocket user message: ', message);
      const taskEvent = JSON.parse(message.body) as TaskEvent;
      this.taskEvents.emit(taskEvent);
    }
  }
}
