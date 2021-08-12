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
import {Environment} from '../environment';
import {ActivatedRoute} from '@angular/router';
import {Location} from '@angular/common';
import {EnvironmentService} from '../environment.service';
import {ClusterService} from '../../clusters/cluster.service';
import {Cluster} from '../../clusters/cluster';
import {EnvironmentReleaseId} from '../../environment-release/environment-release';
import {mergeMap} from 'rxjs/operators';
import {of} from 'rxjs';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {ReleaseService} from '../../release/release.service';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';

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
  environment: Environment;
  environmentsForValidation: Environment[];

  constructor(
    private route: ActivatedRoute,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private releaseService: ReleaseService,
    private clusterService: ClusterService,
    private location: Location,
    private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const clusterId = +this.route.snapshot.queryParamMap.get('clusterId');
    this.clusterService.get(clusterId)
      .subscribe(cluster => {
        this.cluster = cluster;

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
      });

    this.environmentService.getAllEnvironmentsByCluster(clusterId)
      .subscribe((environments) => this.environmentsForValidation = environments);
  }

  private setBreadcrumb() {
    this.breadcrumbService.setItems([
      {label: this.cluster.name, routerLink: '/clusters'},
      {label: (!this.sourceEnvironment ? (this.editMode ? 'edit' : 'new') : 'duplicate') + ' environment'}
    ]);
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
