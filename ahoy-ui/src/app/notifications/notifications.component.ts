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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {LoggerService} from '../util/logger.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Notification} from './notification';
import {NotificationsService} from './notifications.service';
import {Subscription} from 'rxjs';
import {Description} from '../components/description-dialog/description';
import {DialogService} from '../components/dialog.service';
import {MessageService} from 'primeng/api';
import {AppMainComponent} from '../app.main.component';
import {AppComponent} from '../app.component';
import {animate, style, transition, trigger} from '@angular/animations';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
  animations: [
    trigger('topbarActionPanelAnimation', [
      transition(':enter', [
        style({opacity: 0, transform: 'scaleY(0.8)'}),
        animate('.12s cubic-bezier(0, 0, 0.2, 1)', style({opacity: 1, transform: '*'})),
      ]),
      transition(':leave', [
        animate('.1s linear', style({opacity: 0}))
      ])
    ])
  ]
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private notificationsSubscription: Subscription;
  private topBarMenuSubscription: Subscription;
  private readonly NOTIFICATIONS_TO_SHOW = 5;
  notifications: Notification[];
  viewed = true;

  constructor(public appMain: AppMainComponent, public app: AppComponent,
              private log: LoggerService,
              private notificationsService: NotificationsService,
              private snackBar: MatSnackBar,
              private dialogService: DialogService,
              private messageService: MessageService) {
    this.notifications = [];
  }

  ngOnInit() {
    this.notificationsSubscription = this.notificationsService.notifications
      .subscribe((notification) => this.onNotification(notification));

    this.topBarMenuSubscription = this.appMain.topBarMenuChanged.subscribe(topBarEvent => {
      if (topBarEvent.item === 'notifications') {
        this.closedNotifications();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.notificationsSubscription) {
      this.notificationsSubscription.unsubscribe();
    }

    if (this.topBarMenuSubscription) {
      this.topBarMenuSubscription.unsubscribe();
    }
  }

  private onNotification(notification: Notification) {
    this.notifications.push(notification);
    const length = this.notifications.length;
    if (length > this.NOTIFICATIONS_TO_SHOW) {
      this.notifications = this.notifications.slice(length - this.NOTIFICATIONS_TO_SHOW);
    }
    this.viewed = false;
    this.messageService.add({
      severity: notification.error ? 'warn' : 'info',
      summary: notification.error ? 'Warn' : 'Info',
      detail: notification.text
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

  showBadge() {
    return !this.viewed && this.notifications.length > 0;
  }

  showDescription(notification: Notification) {
    this.dialogService.showDescriptionDialog(new Description('Notification', notification.text));
  }
}
