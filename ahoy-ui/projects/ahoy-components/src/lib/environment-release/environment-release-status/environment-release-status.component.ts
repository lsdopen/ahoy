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
import {EnvironmentRelease} from '../environment-release';

@Component({
  selector: 'app-environment-release-status',
  templateUrl: './environment-release-status.component.html',
  styleUrls: ['./environment-release-status.component.scss']
})
export class EnvironmentReleaseStatusComponent {
  @Input() environmentRelease: EnvironmentRelease;

  style(): string {
    switch (this.environmentRelease.status) {
      case 'Healthy':
        return 'status-success';
      case 'Progressing':
      case 'Missing':
        return 'status-warn';
      case 'Degraded':
      case 'Unknown':
        return 'status-error';
    }
    return '';
  }
}
