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

export class Notification {
  text: string;
  viewed: boolean;
  time: Date;
  error: any;
  errorMessage: string;
  errorTrace: string;

  constructor(text: string, error?: any) {
    this.text = text;
    this.viewed = false;
    this.error = error;
    this.time = new Date();

    if (error instanceof HttpErrorResponse && error.status !== 0) {
      error = error.error;

      this.errorMessage = ('message' in error) ? error.message : `Unknown error:  ${error.toString()}`;
      this.errorTrace = ('trace' in error) ? error.trace : undefined;
    }
  }

  minutesAgo() {
    const now = new Date();
    return now.getMinutes() - this.time.getMinutes();
  }
}
