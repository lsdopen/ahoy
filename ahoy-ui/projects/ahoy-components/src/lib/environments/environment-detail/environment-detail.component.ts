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
import {PromoteOptions} from '../../releases/release';
import {OrderUtil} from '../../util/order-util';
import {DuplicateOptions, Environment} from '../environment';
import {EnvironmentService} from '../environment.service';

@Component({
  selector: 'app-environment-detail',
  templateUrl: './environment-detail.component.html',
  styleUrls: ['./environment-detail.component.scss']
})
export class EnvironmentDetailComponent implements OnInit {
  editMode = false;
  environment: Environment;
  environmentsForValidation: Environment[];
  cluster: Cluster;
  clusters: Cluster[] = undefined;
  sourceEnvironment: Environment;
  promoteEnvironmentReleaseId: EnvironmentReleaseId;
  copyEnvironmentConfig: boolean;

  constructor(
    private route: ActivatedRoute,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private releaseManageService: ReleaseManageService,
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
        this.promoteEnvironmentReleaseId = EnvironmentReleaseId.new(environmentId, releaseId);
        this.copyEnvironmentConfig = JSON.parse(this.route.snapshot.queryParamMap.get('copyEnvironmentConfig'));
      }

      this.setBreadcrumb();
    } else {
      this.editMode = true;
      this.environmentService.get(+id)
        .subscribe((env) => {
          this.environment = env;
          this.cluster = env.cluster as Cluster;
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
        {label: this.environment.key},
        {label: 'edit'}
      ]);
    } else if (this.sourceEnvironment) {
      this.breadcrumbService.setItems([
        {label: 'environments', routerLink: '/environments'},
        {label: this.sourceEnvironment.key},
        {label: 'duplicate'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'environments', routerLink: '/environments'},
        {label: 'new'}
      ]);
    }
  }

  showCopyEnvironmentConfig(): boolean {
    return !!this.sourceEnvironment || !!this.promoteEnvironmentReleaseId;
  }

  save() {
    if (!this.editMode) {
      this.environment.orderIndex = OrderUtil.appendIndex(this.environmentsForValidation);
      this.environment.cluster = this.clusterService.link(this.cluster.id);
    } else {
      this.environment.cluster = undefined;
    }

    this.environmentService.save(this.environment).pipe(
      mergeMap((environment: Environment) => {
        if (this.sourceEnvironment) {
          // we're duplicating a source environment
          const duplicateOptions = new DuplicateOptions(this.copyEnvironmentConfig);
          return this.environmentService.duplicate(this.sourceEnvironment, environment, duplicateOptions);
        }
        return of(environment);
      }),
      mergeMap((environment: Environment) => {
        if (this.promoteEnvironmentReleaseId) {
          // we're promoting to this new environment
          const promoteOptions = new PromoteOptions(environment.id, this.copyEnvironmentConfig);
          return this.releaseManageService.promote(this.promoteEnvironmentReleaseId, promoteOptions);
        }
        return of(environment);
      })
    ).subscribe(() => this.location.back());
  }

  cancel() {
    this.environment = undefined;
    this.editMode = false;
    this.location.back();
  }
}
