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
import {ActivatedRoute, Router} from '@angular/router';
import {filter} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Cluster} from '../clusters/cluster';
import {ClusterService} from '../clusters/cluster.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {DialogUtilService} from '../components/dialog-util.service';
import {LoggerService} from '../util/logger.service';
import {OrderUtil} from '../util/order-util';
import {Environment} from './environment';
import {EnvironmentService} from './environment.service';

@Component({
  selector: 'app-environments',
  templateUrl: './environments.component.html',
  styleUrls: ['./environments.component.scss']
})
export class EnvironmentsComponent implements OnInit {
  environments: Environment[] = undefined;
  clusters: Cluster[] = undefined;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private environmentService: EnvironmentService,
              private clusterService: ClusterService,
              private log: LoggerService,
              private dialogUtilService: DialogUtilService,
              private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    this.setBreadcrumb();

    this.clusterService.getAll().subscribe((clusters) => {
      this.clusters = clusters;
    });

    this.getEnvironments();
  }

  private setBreadcrumb() {
    this.breadcrumbService.setItems([{label: 'environments'}]);
  }

  private getEnvironments() {
    this.log.debug('getting all environments');

    this.environmentService.getAll()
      .subscribe((environments) => {
        this.environments = environments;
      });
  }

  delete(event: Event, environment: Environment) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${environment.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = environment.name;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.environmentService.destroy(environment)
        .subscribe(() => this.getEnvironments());
    });
  }

  rowReorder(event: any) {
    const dropIndex = event.dropIndex;
    const environment = this.environments[dropIndex];
    environment.orderIndex = OrderUtil.newIndex(dropIndex, this.environments);
    this.environmentService.updateOrderIndex(environment).subscribe();
  }
}
