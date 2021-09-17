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

import {Component, OnInit} from '@angular/core';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {ApplicationVersion} from '../../applications/application';
import {ApplicationService} from '../../applications/application.service';
import {TabItemFactory} from '../../components/multi-tab/multi-tab.component';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';
import {DockerRegistry, DockerSettings} from './docker-settings';
import {DockerSettingsService} from './docker-settings.service';

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

  constructor(private dockerSettingsService: DockerSettingsService,
              private applicationService: ApplicationService,
              private notificationsService: NotificationsService,
              private breadcrumbService: AppBreadcrumbService) {
    this.breadcrumbService.setItems([
      {label: 'settings'},
      {label: 'docker'}
    ]);
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

    this.applicationService.getAllVersions()
      .subscribe((applicationVersions) => {
        this.applicationVersions = applicationVersions;
      });
  }

  save() {
    const notification = new Notification('Saved docker settings');
    this.dockerSettingsService.save(this.dockerSettings)
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
          .filter((version) => version.dockerRegistry)
          .filter((version) => version.dockerRegistry.id === registry.id)
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
