import {EventEmitter, Injectable, isDevMode} from '@angular/core';
import * as Stomp from 'stompjs';
import * as SockJS from 'sockjs-client';
import {LoggerService} from '../util/logger.service';
import {TaskEvent} from './task-events';

@Injectable({
  providedIn: 'root'
})
export class TaskEventsService {
  private serverUrl = '/ws';
  private readonly RECONNECT_TIMEOUT = 5000;
  private readonly ENABLED = true;
  private stompClient;
  public taskEvents = new EventEmitter<TaskEvent>();

  constructor(private log: LoggerService) {
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
    this.stompClient.connect({}, frame => {
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
