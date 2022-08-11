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

import {Component, OnInit} from '@angular/core';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';
import {SettingsService} from '../settings.service';
import {ArgoSettings} from './argo-settings';

@Component({
  selector: 'app-argo-settings',
  templateUrl: './argo-settings.component.html',
  styleUrls: ['./argo-settings.component.scss']
})
export class ArgoSettingsComponent implements OnInit {
  argoSettings: ArgoSettings;
  hideArgoToken = true;

  constructor(private settingsService: SettingsService,
              private notificationsService: NotificationsService,
              private breadcrumbService: AppBreadcrumbService) {
    this.breadcrumbService.setItems([
      {label: 'settings'},
      {label: 'argo'}
    ]);
  }

  ngOnInit(): void {
    this.settingsService.getArgoSettings()
      .subscribe((argoSettings) => {
        this.argoSettings = argoSettings;
      });
  }

  save() {
    const notification = new Notification('Saved argocd settings');
    this.settingsService.saveArgoSettings(this.argoSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }

  test() {
    this.settingsService.testArgoConnection(this.argoSettings).subscribe();
  }

  updateKnownHosts() {
    this.settingsService.updateArgoKnownHosts().subscribe(() => {
      const text = `Updated SSH Known Hosts in Argo CD`;
      this.notificationsService.notification(new Notification(text));
    });
  }
}
