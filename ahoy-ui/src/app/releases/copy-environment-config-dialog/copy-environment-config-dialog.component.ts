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

import {Component, Inject, OnInit} from '@angular/core';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {Release, ReleaseVersion} from '../release';

@Component({
  selector: 'app-copy-environment-config-dialog',
  templateUrl: './copy-environment-config-dialog.component.html',
  styleUrls: ['./copy-environment-config-dialog.component.scss']
})
export class CopyEnvironmentConfigDialogComponent implements OnInit {
  private environmentRelease: EnvironmentRelease;
  destReleaseVersion: ReleaseVersion;
  releaseVersions: ReleaseVersion[];
  selectedReleaseVersion: ReleaseVersion;

  constructor(@Inject(MAT_DIALOG_DATA) data) {
    this.environmentRelease = data[0];
    this.destReleaseVersion = data[1];
  }

  ngOnInit() {
    this.releaseVersions = (this.environmentRelease.release as Release).releaseVersions
      .filter(rel => rel.id !== this.destReleaseVersion.id);
  }
}
