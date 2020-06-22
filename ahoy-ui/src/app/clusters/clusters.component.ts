/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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
import {Cluster} from './cluster';
import {ActivatedRoute} from '@angular/router';
import {ClusterService} from './cluster.service';
import {LoggerService} from '../util/logger.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {filter} from 'rxjs/operators';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-clusters',
  templateUrl: './clusters.component.html',
  styleUrls: ['./clusters.component.scss']
})
export class ClustersComponent implements OnInit {
  clusters: Cluster[] = undefined;

  constructor(private route: ActivatedRoute,
              private clusterService: ClusterService,
              private log: LoggerService,
              private dialogService: DialogService) {
  }

  ngOnInit() {
    this.getAllClusters();
  }

  private getAllClusters() {
    this.log.debug('getting all clusters');
    this.clusterService.getAll()
      .subscribe(clusters => this.clusters = clusters);
  }

  delete(cluster: Cluster) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${cluster.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = cluster.name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.clusterService.destroy(cluster)
        .subscribe(() => this.getAllClusters());
    });
  }
}
