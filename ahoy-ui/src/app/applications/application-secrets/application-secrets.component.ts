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

import {AfterContentChecked, ChangeDetectorRef, Component, Input} from '@angular/core';
import {ControlContainer, NgForm} from '@angular/forms';
import {ApplicationEnvironmentVariable, ApplicationSecret, ApplicationVolume} from '../application';

@Component({
  selector: 'app-application-secrets',
  templateUrl: './application-secrets.component.html',
  styleUrls: ['./application-secrets.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationSecretsComponent implements AfterContentChecked {
  @Input() secrets: ApplicationSecret[];
  @Input() volumes: ApplicationVolume[];
  @Input() environmentVariables: ApplicationEnvironmentVariable[];
  @Input() routeTlsSecretName: string;

  selectedSecretIndex = 0;

  constructor(private cd: ChangeDetectorRef) {
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  addSecret() {
    const applicationSecret = new ApplicationSecret();
    applicationSecret.type = 'Generic';
    applicationSecret.data = {};
    this.secrets.push(applicationSecret);
    setTimeout(() => this.selectedSecretIndex = this.secrets.length - 1);
  }

  deleteSecret() {
    this.secrets.splice(this.selectedSecretIndex, 1);
    setTimeout(() => {
      if (this.selectedSecretIndex === this.secrets.length) {
        // only move one tab back if its the last tab
        this.selectedSecretIndex = this.secrets.length - 1;
      }
    });
  }

  secretInUse(): boolean {
    const secret = this.secrets[this.selectedSecretIndex];
    if (secret && secret.name) {
      const inUseInVolumes = this.volumes
        .filter(volume => volume.type === 'Secret' && volume.secretName === secret.name).length > 0;
      const inUseInEnvironmentVariables = this.environmentVariables
        .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
      return inUseInVolumes || inUseInEnvironmentVariables ||
        (this.routeTlsSecretName !== undefined ? this.routeTlsSecretName === secret.name : false);
    }
    return false;
  }
}
