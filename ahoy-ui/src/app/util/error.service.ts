import {ErrorHandler, Injectable, NgZone} from '@angular/core';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {LoggerService} from './logger.service';
import {HttpErrorResponse} from '@angular/common/http';

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
      this.notificationsService.notification(new Notification(text, true));
    });
  }
}
