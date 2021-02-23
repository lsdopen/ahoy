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

import {EventEmitter, Injectable, isDevMode} from '@angular/core';
import * as Stomp from 'stompjs';
import * as SockJS from 'sockjs-client';
import {LoggerService} from '../util/logger.service';
import {TaskEvent} from './task-events';
import {AuthService} from '../util/auth.service';

@Injectable({
  providedIn: 'root'
})
export class TaskEventsService {
  private serverUrl = '/ws';
  private readonly RECONNECT_TIMEOUT = 5000;
  private readonly ENABLED = true;
  private stompClient;
  public taskEvents = new EventEmitter<TaskEvent>();

  constructor(private authService: AuthService,
              private log: LoggerService) {
    if (isDevMode()) {
      this.serverUrl = 'http://localhost:8080' + this.serverUrl;
    }

    if (this.ENABLED) {
      this.connectAndReconnect();
    }
  }

  private connectAndReconnect() {
    const ws = new SockJS(this.serverUrl);
    this.stompClient = Stomp.over(ws);
    this.stompClient.debug = null;
    const that = this;
    this.stompClient.connect({'X-Authorization': 'Bearer ' + this.authService.accessToken()}, (frame) => {
      this.log.debug('Websocket connection established:', ws);
      that.stompClient.subscribe('/events', (message) => that.receivedAppMessage(message));
      that.stompClient.subscribe('/user/events', (message) => that.receivedUserMessage(message));
    }, (error) => {
      this.log.warn('Websocket connection failed', error);
      setTimeout(() => this.connectAndReconnect(), this.RECONNECT_TIMEOUT);
    });
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
