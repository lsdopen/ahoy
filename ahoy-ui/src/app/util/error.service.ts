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

import {HttpErrorResponse} from '@angular/common/http';
import {ErrorHandler, Injectable, NgZone} from '@angular/core';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {LoggerService} from './logger.service';

@Injectable()
export class ErrorService implements ErrorHandler {

  constructor(private notificationsService: NotificationsService,
              private log: LoggerService,
              private zone: NgZone) {
  }

  handleError(error: any): void {
    this.log.error('Uncaught error', error);

    if (error instanceof HttpErrorResponse && error.status !== 0) {
      error = error.error;
    }

    const text = ('message' in error) ? error.message : `Unknown error:  ${error.toString()}`;

    this.zone.run(() => {
      this.notificationsService.notification(new Notification(text, error));
    });
  }
}
