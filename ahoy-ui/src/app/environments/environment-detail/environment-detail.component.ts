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

import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {of} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {Cluster} from '../../clusters/cluster';
import {ClusterService} from '../../clusters/cluster.service';
import {EnvironmentReleaseId} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {ReleaseManageService} from '../../release-manage/release-manage.service';
import {Environment} from '../environment';
import {EnvironmentService} from '../environment.service';

@Component({
  selector: 'app-environment-detail',
  templateUrl: './environment-detail.component.html',
  styleUrls: ['./environment-detail.component.scss']
})
export class EnvironmentDetailComponent implements OnInit {
  private environmentReleaseId: EnvironmentReleaseId;
  editMode = false;
  sourceEnvironment: Environment;
  cluster: Cluster;
  clusters: Cluster[] = undefined;
  environment: Environment;
  environmentsForValidation: Environment[];

  constructor(
    private route: ActivatedRoute,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private releaseService: ReleaseManageService,
    private clusterService: ClusterService,
    private location: Location,
    private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id === 'new') {
      this.environment = new Environment();
      const sourceEnvironmentId = +this.route.snapshot.queryParamMap.get('sourceEnvironmentId');
      if (sourceEnvironmentId) {
        this.environmentService.get(sourceEnvironmentId)
          .subscribe((env) => {
            this.sourceEnvironment = env;
            this.setBreadcrumb();
          });
      }

      const environmentId = +this.route.snapshot.queryParamMap.get('environmentId');
      const releaseId = +this.route.snapshot.queryParamMap.get('releaseId');
      if (environmentId && releaseId) {
        this.environmentReleaseId = EnvironmentReleaseId.new(environmentId, releaseId);
      }

      this.setBreadcrumb();
    } else {
      this.editMode = true;
      this.environmentService.get(+id)
        .subscribe((env) => {
          this.environment = env;
          this.setBreadcrumb();
        });
    }

    this.clusterService.getAll().subscribe((clusters) => {
      this.clusters = clusters;
    });

    this.environmentService.getAll()
      .subscribe((environments) => this.environmentsForValidation = environments);
  }

  private setBreadcrumb() {
    if (this.editMode) {
      this.breadcrumbService.setItems([
        {label: 'environments', routerLink: '/environments'},
        {label: this.environment.name},
        {label: 'edit'}
      ]);
    } else if (this.sourceEnvironment) {
      this.breadcrumbService.setItems([
        {label: 'environments', routerLink: '/environments'},
        {label: this.sourceEnvironment.name},
        {label: 'duplicate'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'environments', routerLink: '/environments'},
        {label: 'new'}
      ]);
    }
  }

  save() {
    if (!this.editMode) {
      this.environment.cluster = this.cluster;
      this.environmentService.create(this.environment).pipe(
        mergeMap((environment: Environment) => {
          if (this.sourceEnvironment) {
            // we're duplicating a source environment
            return this.environmentService.duplicate(this.sourceEnvironment, environment);
          }
          return of(environment);
        }),
        mergeMap((environment: Environment) => {
          if (this.environmentReleaseId) {
            // we're promoting to this new environment
            return this.releaseService.promote(this.environmentReleaseId, environment.id);
          }
          return of(environment);
        })
      ).subscribe(() => this.location.back());
    } else {
      this.location.back();
    }
  }

  cancel() {
    this.environment = undefined;
    this.editMode = false;
    this.location.back();
  }
}
