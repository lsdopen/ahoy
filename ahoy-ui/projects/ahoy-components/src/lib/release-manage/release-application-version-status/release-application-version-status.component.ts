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

import {Component, Input} from '@angular/core';
import {ApplicationVersion} from '../../applications/application';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {ReleaseVersion} from '../../releases/release';

@Component({
  selector: 'app-release-application-version-status',
  templateUrl: './release-application-version-status.component.html',
  styleUrls: ['./release-application-version-status.component.scss']
})
export class ReleaseApplicationVersionStatusComponent {
  @Input() environmentRelease: EnvironmentRelease;
  @Input() releaseVersion: ReleaseVersion;
  @Input() applicationVersion: ApplicationVersion;

  status(): string {
    if (!this.environmentRelease.deployed) {
      return '';
    }

    if (this.environmentRelease.currentReleaseVersion.version !== this.releaseVersion.version) {
      return '';
    }

    if (!this.applicationVersion.status) {
      return 'Missing';
    }

    return this.applicationVersion.status.status;
  }

  style(): string {
    if (!this.environmentRelease.deployed) {
      return '';
    }

    if (this.environmentRelease.currentReleaseVersion.version !== this.releaseVersion.version) {
      return '';
    }

    if (!this.applicationVersion.status) {
      return 'status-error';
    }

    if (this.applicationVersion.status.status === 'Healthy') {
      return 'status-success';
    }

    return 'status-warn';
  }
}
