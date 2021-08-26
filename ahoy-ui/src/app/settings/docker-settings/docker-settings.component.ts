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

import {AfterContentChecked, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';
import {DockerRegistry, DockerSettings} from './docker-settings';
import {DockerSettingsService} from './docker-settings.service';

@Component({
  selector: 'app-docker-settings',
  templateUrl: './docker-settings.component.html',
  styleUrls: ['./docker-settings.component.scss']
})
export class DockerSettingsComponent implements OnInit, AfterContentChecked {
  dockerSettings: DockerSettings;
  hideDockerPassword = true;
  selectedIndex: number;

  constructor(private cd: ChangeDetectorRef,
              private dockerSettingsService: DockerSettingsService,
              private notificationsService: NotificationsService,
              private breadcrumbService: AppBreadcrumbService) {
    this.breadcrumbService.setItems([
      {label: 'settings'},
      {label: 'docker'}
    ]);
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  ngOnInit(): void {
    this.dockerSettingsService.get()
      .subscribe((dockerSettings) => {
        this.dockerSettings = dockerSettings;
        if (!this.dockerSettings.dockerRegistries) {
          this.dockerSettings.dockerRegistries = [];
        } else {
          this.selectedIndex = 0;
        }
      });
  }

  save() {
    const notification = new Notification('Saved docker settings');
    this.dockerSettingsService.save(this.dockerSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }

  addDockerRegistry() {
    this.dockerSettings.dockerRegistries.push(new DockerRegistry());
    setTimeout(() => this.selectedIndex = this.dockerSettings.dockerRegistries.length - 1);
  }

  deleteRegistry() {
    this.dockerSettings.dockerRegistries.splice(this.selectedIndex, 1);
    setTimeout(() => {
      if (this.selectedIndex === this.dockerSettings.dockerRegistries.length) {
        // only move one tab back if its the last tab
        this.selectedIndex = this.dockerSettings.dockerRegistries.length - 1;
      }
    });
  }
}
