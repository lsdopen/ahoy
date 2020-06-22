import {EventEmitter, Injectable} from '@angular/core';
import {LoggerService} from '../util/logger.service';
import {Notification} from './notification';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService {
  public notifications = new EventEmitter<Notification>();
  public progress = false;

  constructor(private log: LoggerService) {
  }

  public showProgress(progress: boolean) {
    this.progress = progress;
  }

  public notification(notification: Notification) {
    this.notifications.emit(notification);
  }
}
