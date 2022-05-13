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
import {ActivatedRoute} from '@angular/router';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {ClusterService} from '../clusters/cluster.service';
import {Environment} from '../environments/environment';
import {EnvironmentService} from '../environments/environment.service';
import {RecentReleasesService} from '../release-manage/recent-releases.service';
import {Role} from '../util/auth';
import {LoggerService} from '../util/logger.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  Role = Role;
  environments: Environment[] = [];
  clusterCount = 0;

  constructor(private route: ActivatedRoute,
              private environmentService: EnvironmentService,
              private clusterService: ClusterService,
              private recentReleasesService: RecentReleasesService,
              private log: LoggerService,
              private breadcrumbService: AppBreadcrumbService) {
    this.breadcrumbService.setItems([{label: 'dashboard'}]);
  }

  ngOnInit() {
    this.clusterService.count().subscribe((count) => {
      this.clusterCount = count;
    });

    this.recentReleasesService.refresh();

    this.getEnvironments();
  }

  private getEnvironments() {
    this.log.debug('getting all environments');

    this.environmentService.getAll()
      .subscribe((environments) => {
        this.environments = environments;
      });
  }
}
