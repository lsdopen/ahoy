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
import {Environment} from '../../environments/environment';
import {EnvironmentService} from '../../environments/environment.service';
import {Release} from '../../releases/release';

@Component({
  selector: 'app-promote-dialog',
  templateUrl: './add-to-environment-dialog.component.html',
  styleUrls: ['./add-to-environment-dialog.component.scss']
})
export class AddToEnvironmentDialogComponent implements OnInit {
  release: Release;
  environments: Environment[];
  selected: Environment;

  constructor(private environmentService: EnvironmentService,
              public ref: DynamicDialogRef,
              public config: DynamicDialogConfig) {
    const data = config.data;
    this.release = data.release;
  }

  ngOnInit() {
    this.environmentService.getAllForPromotion(this.release.id)
      .subscribe(environments => this.environments = environments);
  }

  cancel() {
    this.ref.destroy();
  }

  close(result: any) {
    this.ref.close(result);
  }
}
