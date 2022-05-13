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

import {Injectable, isDevMode} from '@angular/core';
import {EventSourcePolyfill} from 'event-source-polyfill/src/eventsource.min.js';
import {Observable, Observer} from 'rxjs';
import {AuthService} from './auth.service';
import {LoggerService} from './logger.service';

@Injectable({
  providedIn: 'root'
})
export class EventSourceService {
  private readonly MAX_RETRIES = 3;
  private appsUrl = '';

  constructor(private authService: AuthService,
              private log: LoggerService) {
    if (isDevMode()) {
      this.appsUrl = 'http://localhost:8080' + this.appsUrl;
    }
  }


  public getEvents<T>(url: string): Observable<T> {
    return new Observable((observer: Observer<T>) => {
      this.log.debug('Establishing event source connection to: ' + url);

      let eventSource = new EventSourcePolyfill(this.appsUrl + url, {
        headers: {Authorization: 'Bearer ' + this.authService.accessToken()}
      });
      eventSource.onmessage = msg => observer.next(JSON.parse(msg.data));
      eventSource.onerror = e => () => observer.error(e);

      let retries = 0;
      const interval = setInterval(() => {
        if (eventSource && eventSource.readyState === ReadyState.CONNECTING) {
          this.log.debug('event source connecting...');
          retries++;
          if (retries === this.MAX_RETRIES) {
            eventSource.close();
          }
        }
        if (eventSource && eventSource.readyState === ReadyState.CLOSED) {
          observer.error(new Error('Event source connection closed unexpectedly'));
        }
      }, 500);

      return () => {
        this.log.debug('Event source connection complete: ' + url);
        clearInterval(interval);
        eventSource.close();
        eventSource = null;
      };
    });
  }
}

export enum ReadyState {
  CONNECTING = 0,
  OPEN = 1,
  CLOSED = 2,
  DONE = 4
}
