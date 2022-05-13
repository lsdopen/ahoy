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

import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentRelease} from '../environment-release';

@Component({
  selector: 'app-environment-release-applications-status',
  templateUrl: './environment-release-applications-status.component.html',
  styleUrls: ['./environment-release-applications-status.component.scss']
})
export class EnvironmentReleaseApplicationsStatusComponent implements OnInit {
  @Input() environmentRelease: EnvironmentRelease;
  private applicationsReady: number;
  private applicationsTotal: number;

  ngOnInit() {
    this.applicationsReady = this.environmentRelease.applicationsReady ?
      this.environmentRelease.applicationsReady : 0;
    this.applicationsTotal = this.environmentRelease.currentReleaseVersion ?
      this.environmentRelease.currentReleaseVersion.applicationVersions.length : 0;
  }

  style(): string {
    if (this.applicationsReady === 0 && this.applicationsTotal > 0) {
      return 'status-error';

    } else if (this.applicationsReady < this.applicationsTotal) {
      return 'status-warn';

    } else {
      return 'status-success';
    }
  }
}
