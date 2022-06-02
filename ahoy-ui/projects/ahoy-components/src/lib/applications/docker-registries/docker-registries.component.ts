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

import {Component, Input, OnInit} from '@angular/core';
import {DockerRegistry} from '../../settings/docker-settings/docker-settings';
import {SettingsService} from '../../settings/settings.service';
import {ApplicationSpec} from '../application';

@Component({
  selector: 'app-docker-registries',
  templateUrl: './docker-registries.component.html',
  styleUrls: ['./docker-registries.component.scss']
})
export class DockerRegistriesComponent implements OnInit {
  @Input() applicationSpec: ApplicationSpec;
  dockerRegistries: DockerRegistry[];

  constructor(private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.settingsService.getDockerSettings()
      .subscribe((settings) => {
        this.dockerRegistries = settings.dockerRegistries ? settings.dockerRegistries : [];
      });
  }

  dockerRegistryExists(name: string) {
    return this.dockerRegistries &&
      this.dockerRegistries.find((dockerRegistry) => name === dockerRegistry.name);
  }
}
