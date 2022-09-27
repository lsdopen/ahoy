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
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {Cluster} from '../../clusters/cluster';
import {ClusterService} from '../../clusters/cluster.service';
import {Environment} from '../environment';

@Component({
  selector: 'app-move-dialog',
  templateUrl: './move-dialog.component.html',
  styleUrls: ['./move-dialog.component.scss']
})
export class MoveDialogComponent implements OnInit {
  environment: Environment;
  clusters: Cluster[];
  result = new Result();

  constructor(public ref: DynamicDialogRef,
              public config: DynamicDialogConfig,
              private clusterService: ClusterService) {
    const data = config.data;
    this.environment = data.environment;
  }

  ngOnInit(): void {
    this.clusterService.getAll().subscribe((clusters) => {
      this.clusters = clusters.filter(c => c.id !== (this.environment.cluster as Cluster).id);
    });
  }

  close(result: any) {
    this.ref.close(result);
  }
}

export class Result {
  destCluster: Cluster;
  redeployReleases = true;
}
