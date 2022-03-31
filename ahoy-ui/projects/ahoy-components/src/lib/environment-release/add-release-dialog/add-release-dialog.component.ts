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
import {Release} from '../../releases/release';
import {ReleaseService} from '../../releases/release.service';

@Component({
  selector: 'app-add-release-dialog',
  templateUrl: './add-release-dialog.component.html',
  styleUrls: ['./add-release-dialog.component.scss']
})
export class AddReleaseDialogComponent implements OnInit {
  releases: Release[];
  selected: Release;
  environment: Environment;

  constructor(private releasesService: ReleaseService,
              public ref: DynamicDialogRef,
              public config: DynamicDialogConfig) {
    this.environment = config.data;
  }

  ngOnInit() {
    this.releasesService.getAllForAdd(this.environment.id)
      .subscribe(rels => this.releases = rels);
  }

  close(result: any) {
    this.ref.close(result);
  }
}
