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

import {State, TaskProgressEvent} from '../task/task';
import {ErrorUtil} from '../util/error-util';

export class Notification {
  text: string;
  viewed: boolean;
  time: Date;
  errorMessage: string;
  errorTrace: string;

  id: string;
  message: string;
  state: State;

  constructor(text: string, error?: any) {
    this.text = text;
    this.viewed = false;
    this.time = new Date();
    this.id = this.time.getTime().toString();

    if (ErrorUtil.is500Error(error) && error.error) {
      error = error.error;
    }

    if (error) {
      this.state = State.ERROR;
      this.errorMessage = ('message' in error) ? error.message : `Unknown error:  ${error.toString()}`;
      this.errorTrace = ('trace' in error) ? error.trace : undefined;

    } else {
      this.state = State.NOTIFICATION;
    }
  }

  public static createFromProgress(taskProgressEvent: TaskProgressEvent): Notification {
    const notification = new Notification(taskProgressEvent.status, null);
    notification.id = taskProgressEvent.id;
    notification.state = taskProgressEvent.state;
    notification.time = new Date(taskProgressEvent.time);
    return notification;
  }

  public setProgress(taskProgressEvent: TaskProgressEvent): void {
    this.state = taskProgressEvent.state;
    if (taskProgressEvent.status) {
      this.text = taskProgressEvent.status;
    }
    this.message = taskProgressEvent.message;
    if (this.state === State.ERROR) {
      this.errorMessage = taskProgressEvent.status;
      this.errorTrace = taskProgressEvent.trace;
    }
  }

  minutesAgo() {
    const now = new Date();
    return this.minutesSinceEpoch(now) - this.minutesSinceEpoch(this.time);
  }

  minutesSinceEpoch(d: Date) {
    return Math.floor(d.getTime() / (1000.0 * 60));
  }
}
