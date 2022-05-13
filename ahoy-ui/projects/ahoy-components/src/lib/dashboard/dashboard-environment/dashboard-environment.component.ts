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
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {Environment} from '../../environments/environment';
import {TaskEvent} from '../../taskevents/task-events';
import {Role} from '../../util/auth';

@Component({
  selector: 'app-dashboard-environment',
  templateUrl: './dashboard-environment.component.html',
  styleUrls: ['./dashboard-environment.component.scss']
})
export class DashboardEnvironmentComponent implements OnInit {
  Role = Role;
  @Input() environment: Environment;
  environmentReleases: EnvironmentRelease[];

  constructor(private environmentReleaseService: EnvironmentReleaseService) {
  }

  ngOnInit() {
    this.getReleases();
  }

  private getReleases() {
    this.environmentReleaseService.getReleasesByEnvironment(this.environment.id)
      .subscribe(environmentReleases => this.environmentReleases = environmentReleases);
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.environment.id === statusChangedEvent.environmentReleaseId.environmentId) {
        setTimeout(() => {
          this.getReleases();
        }, 1000);
      }
    }
  }
}
