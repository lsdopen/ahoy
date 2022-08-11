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
import {filter} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {DialogUtilService} from '../components/dialog-util.service';
import {Role} from '../util/auth';
import {LoggerService} from '../util/logger.service';
import {Cluster} from './cluster';
import {ClusterService} from './cluster.service';

@Component({
  selector: 'app-clusters',
  templateUrl: './clusters.component.html',
  styleUrls: ['./clusters.component.scss']
})
export class ClustersComponent implements OnInit {
  Role = Role;
  clusters: Cluster[] = undefined;

  constructor(private route: ActivatedRoute,
              private clusterService: ClusterService,
              private log: LoggerService,
              private dialogUtilService: DialogUtilService,
              private breadcrumbService: AppBreadcrumbService) {

    this.breadcrumbService.setItems([{label: 'clusters'}]);
  }

  ngOnInit() {
    this.getAllClusters();
  }

  private getAllClusters() {
    this.log.debug('getting all clusters');
    this.clusterService.getAll()
      .subscribe(clusters => this.clusters = clusters);
  }

  delete(event: Event, cluster: Cluster) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${cluster.name}?`);
    confirmation.infoText = 'Please note: all deployed releases will be undeployed';
    confirmation.verify = true;
    confirmation.verifyText = cluster.name;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.clusterService.deleteCascading(cluster)
        .subscribe(() => this.getAllClusters());
    });
  }
}
