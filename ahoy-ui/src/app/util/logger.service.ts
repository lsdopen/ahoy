/* tslint:disable:no-console */
import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private logLevel: LogLevel = LogLevel.DEBUG;

  constructor() {
  }

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
