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

/* tslint:disable:no-console */
import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private logLevel: LogLevel = LogLevel.DEBUG;

  private static format(date: Date): string {
    const hours = date.getHours();
    const minutes = date.getMinutes();
    const seconds = date.getSeconds();
    const millis = date.getMilliseconds();

    let format = '';
    format += (hours < 10) ? '0' + hours : hours;
    format += ':' + ((minutes < 10) ? '0' + minutes : minutes);
    format += ':' + ((seconds < 10) ? '0' + seconds : seconds);
    format += '.' + millis;
    return format;
  }

  trace(message?: any, ...args: any[]) {
    this.log(LogLevel.TRACE, message, ...args);
  }

  debug(message?: any, ...args: any[]) {
    this.log(LogLevel.DEBUG, message, ...args);
  }

  info(message?: any, ...args: any[]) {
    this.log(LogLevel.INFO, message, ...args);
  }

  warn(message?: any, ...args: any[]) {
    this.log(LogLevel.WARN, message, ...args);
  }

  error(message?: any, ...args: any[]) {
    this.log(LogLevel.ERROR, message, ...args);
  }

  private log(level: LogLevel, message?: any, ...args: any[]): void {
    if (level.valueOf() >= this.logLevel) {
      const pattern = LoggerService.format(new Date()) + ' ' + LogLevel[level] + ' - ';
      switch (level) {
        case LogLevel.TRACE:
          console.trace(pattern + message, ...args);
          break;
        case LogLevel.DEBUG:
          console.debug(pattern + message, ...args);
          break;
        case LogLevel.INFO:
          console.info(pattern + message, ...args);
          break;
        case LogLevel.WARN:
          console.warn(pattern + message, ...args);
          break;
        case LogLevel.ERROR:
          console.error(pattern + message, ...args);
          break;
      }
    }
  }
}

export enum LogLevel {
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR
}
