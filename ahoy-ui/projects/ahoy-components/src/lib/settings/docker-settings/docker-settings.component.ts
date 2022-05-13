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
import {ApplicationVersion} from '../../applications/application';
import {ApplicationService} from '../../applications/application.service';
import {TabItemFactory} from '../../components/multi-tab/multi-tab.component';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';
import {SettingsService} from '../settings.service';
import {DockerRegistry, DockerSettings} from './docker-settings';

@Component({
  selector: 'app-docker-settings',
  templateUrl: './docker-settings.component.html',
  styleUrls: ['./docker-settings.component.scss']
})
export class DockerSettingsComponent implements OnInit {
  dockerSettings: DockerSettings;
  hideDockerPassword = true;
  selectedIndex: number;
  applicationVersions: ApplicationVersion[];

  constructor(private settingsService: SettingsService,
              private applicationService: ApplicationService,
              private notificationsService: NotificationsService,
              private breadcrumbService: AppBreadcrumbService) {
    this.breadcrumbService.setItems([
      {label: 'settings'},
      {label: 'docker'}
    ]);
  }

  ngOnInit(): void {
    this.settingsService.getDockerSettings()
      .subscribe((dockerSettings) => {
        this.dockerSettings = dockerSettings;
        if (!this.dockerSettings.dockerRegistries) {
          this.dockerSettings.dockerRegistries = [];
        } else {
          this.selectedIndex = 0;
        }
      });

    this.applicationService.getAllVersionsSummary()
      .subscribe((applicationVersions) => {
        this.applicationVersions = applicationVersions;
      });
  }

  save() {
    const notification = new Notification('Saved docker settings');
    this.settingsService.saveDockerSettings(this.dockerSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }

  dockerRegistryFactory(): TabItemFactory<DockerRegistry> {
    return (): DockerRegistry => {
      return new DockerRegistry();
    };
  }

  registryInUse() {
    return (registry: DockerRegistry): boolean => {
      if (this.applicationVersions) {
        return this.applicationVersions
          .filter((version) => version.spec.dockerRegistryName)
          .filter((version) => version.spec.dockerRegistryName === registry.name)
          .length > 0;
      }
      return false;
    };
  }

  registryInUseTooltip() {
    return (registry: DockerRegistry): string => {
      return `${registry.name} in use`;
    };
  }
}
