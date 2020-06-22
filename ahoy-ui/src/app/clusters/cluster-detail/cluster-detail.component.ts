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
import {Cluster} from '../cluster';
import {ActivatedRoute} from '@angular/router';
import {Location} from '@angular/common';
import {LoggerService} from '../../util/logger.service';
import {ClusterService} from '../cluster.service';

@Component({
  selector: 'app-cluster-detail',
  templateUrl: './cluster-detail.component.html',
  styleUrls: ['./cluster-detail.component.scss']
})
export class ClusterDetailComponent implements OnInit {
  types = [
    {value: 'OPENSHIFT', viewValue: 'Openshift'},
    {value: 'KUBERNETES', viewValue: 'Kubernetes'},
    {value: 'NOOP', viewValue: 'Noop'}
  ];

  cluster: Cluster;
  hideToken = true;

  constructor(private route: ActivatedRoute,
              private clusterService: ClusterService,
              private location: Location,
              private log: LoggerService) {
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id === 'new') {
      this.cluster = new Cluster();

    } else {
      this.clusterService.get(+id)
        .subscribe(cluster => this.cluster = cluster);
    }
  }

  save() {
    this.clusterService.save(this.cluster)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.cluster = undefined;
    this.location.back();
  }
}
