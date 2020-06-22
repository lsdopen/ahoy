import {Component, OnDestroy, OnInit} from '@angular/core';
import {LoggerService} from '../util/logger.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Notification} from './notification';
import {NotificationsService} from './notifications.service';
import {Subscription} from 'rxjs';
import {SnackbarComponent} from './snackbar/snackbar.component';
import {Description} from '../components/description-dialog/description';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss']
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private notificationsSubscription: Subscription;
  private readonly NOTIFICATIONS_TO_SHOW = 5;
  notifications: Notification[];
  viewed = true;

  constructor(private log: LoggerService,
              private notificationsService: NotificationsService,
              private snackBar: MatSnackBar,
              private dialogService: DialogService) {
    this.notifications = [];
  }

  ngOnInit() {
    this.notificationsSubscription = this.notificationsService.notifications
      .subscribe((notification) => this.onNotification(notification));
  }

  ngOnDestroy(): void {
    this.notificationsSubscription.unsubscribe();
  }

  showProgress(): boolean {
    return this.notificationsService.progress;
  }

  private onNotification(notification: Notification) {
    this.notifications.push(notification);
    const length = this.notifications.length;
    if (length > this.NOTIFICATIONS_TO_SHOW) {
      this.notifications = this.notifications.slice(length - this.NOTIFICATIONS_TO_SHOW);
    }
    this.viewed = false;
    this.snackBar.openFromComponent(
      SnackbarComponent, {
        duration: 5000,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        data: {notification}
      });
  }

  closedNotifications() {
    for (const notification of this.notifications) {
      notification.viewed = true;
    }
    this.viewed = true;
  }

  unreadNotifications(): number {
    return this.notifications.filter((n) => !n.viewed).length;
  }

  showDescription(notification: Notification) {
    this.dialogService.showDescriptionDialog(new Description('Notification', notification.text));
  }
}
