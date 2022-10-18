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

import {animate, style, transition, trigger} from '@angular/animations';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {MessageService} from 'primeng/api';
import {Description, DialogUtilService, LoggerService, Notification, NotificationsService, ProgressService, State, TaskEvent} from 'projects/ahoy-components/src/public-api';
import {Subscription} from 'rxjs';
import {AppComponent} from '../app.component';
import {AppMainComponent} from '../app.main.component';
import {KeyValue} from '@angular/common';

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
  notifications = new Map<string, Notification>();
  viewed = true;
  timeOrder = (a: KeyValue<string, Notification>, b: KeyValue<string, Notification>): number => {
    return b.value.time.getTime() - a.value.time.getTime();
  }

  constructor(public appMain: AppMainComponent, public app: AppComponent,
              private log: LoggerService,
              private notificationsService: NotificationsService,
              private dialogUtilService: DialogUtilService,
              private messageService: MessageService,
              private progressService: ProgressService) {
  }

  ngOnInit() {
    this.notificationsSubscription = this.notificationsService.notifications
      .subscribe((notification) => this.addNotification(notification));

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

  private addNotification(notification: Notification) {
    this.notifications.set(notification.id, notification);

    const notificationsArr = Array.from(this.notifications.values());
    notificationsArr.sort((a, b) => b.time.getTime() - a.time.getTime());
    for (let i = this.NOTIFICATIONS_TO_SHOW; i < notificationsArr.length; i++) {
      this.notifications.delete(notificationsArr[i].id);
    }
    this.viewed = false;
    this.showMessage(notification);
  }

  closedNotifications() {
    for (const notification of this.notifications.values()) {
      notification.viewed = true;
    }
    this.viewed = true;
  }

  unreadNotifications(): number {
    return Array.from(this.notifications.values()).filter((n) => !n.viewed).length;
  }

  showBadge() {
    return !this.viewed && this.notifications.size > 0;
  }

  showProgress(): boolean {
    return this.progressService.progress || this.notificationsInProgress();
  }

  showDescription(notification: Notification) {
    this.dialogUtilService.showDescriptionDialog(new Description('Notification', notification.text, notification.errorTrace));
  }

  notificationsInProgress(): boolean {
    return Array.from(this.notifications.values()).find(n => n.state === State.IN_PROGRESS) !== undefined;
  }

  taskEventOccurred(event: TaskEvent) {
    const taskProgressEvent = event.taskProgressEvent;
    if (taskProgressEvent) {
      let notification = this.notifications.get(taskProgressEvent.id);
      if (notification) {
        notification.setProgress(taskProgressEvent);
      } else {
        notification = Notification.createFromProgress(taskProgressEvent);
      }

      this.addNotification(notification);
    }
  }

  notificationType(notification: Notification): string {
    switch (notification.state) {
      case State.NOTIFICATION:
      case State.DONE:
        return 'Info';
      case State.WAITING:
        return 'Queued';
      case State.IN_PROGRESS:
        return 'Task';
      case State.ERROR:
        return 'Error';
    }
  }

  notificationIcon(notification: Notification): string {
    switch (notification.state) {
      case State.NOTIFICATION:
      case State.DONE:
        return 'pi-info-circle';
      case State.WAITING:
      case State.IN_PROGRESS:
        return 'pi-spin pi-spinner';
      case State.ERROR:
        return 'pi-exclamation-triangle';
    }
  }

  private showMessage(notification: Notification) {
    switch (notification.state) {
      case State.NOTIFICATION:
      case State.DONE:
        this.messageService.add({summary: 'Info', severity: 'info', detail: notification.text});
        break;
      case State.ERROR:
        this.messageService.add({summary: 'Error', severity: 'error', detail: notification.text});
        break;
    }
  }
}
