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

import {Component} from '@angular/core';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {Environment, MoveOptions} from '../environment';
import {Cluster} from '../../clusters/cluster';

@Component({
  selector: 'app-move-dialog',
  templateUrl: './move-dialog.component.html',
  styleUrls: ['./move-dialog.component.scss']
})
export class MoveDialogComponent {
  selectedEnvironment: Environment;
  clusters: Cluster[];
  moveOptions = new MoveOptions();

  constructor(public ref: DynamicDialogRef,
              public config: DynamicDialogConfig) {
    const data = config.data;
    this.selectedEnvironment = data.selectedEnvironment;
    this.clusters = data.clusters
      .filter(c => c.id !== (this.selectedEnvironment.cluster as Cluster).id);
  }

  close(result: any) {
    this.ref.close(result);
  }
}
