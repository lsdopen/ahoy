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
import {ControlContainer, NgForm} from '@angular/forms';
import {LoggerService} from '../../util/logger.service';
import {ApplicationEnvironmentVariable, ApplicationSecret} from '../application';

@Component({
  selector: 'app-application-env-variables',
  templateUrl: './application-env-variables.component.html',
  styleUrls: ['./application-env-variables.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationEnvVariablesComponent {
  @Input() environmentVariables: ApplicationEnvironmentVariable[];
  @Input() secrets: ApplicationSecret[];
  @Input() containerSpecIndex: number;
  type = 'Value';
  key: string;
  value: string;
  secret: ApplicationSecret;
  secretKey: string;
  editingSecret: ApplicationSecret;
  editingSecretKey: string;
  editingEnvVar: ApplicationEnvironmentVariable;

  constructor(private log: LoggerService) {
  }

  addEnvironmentVariable() {
    if (this.key) {
      const existingEnvVar = this.environmentVariables.find(envVar => envVar.key === this.key);
      if (this.type === 'Value' && this.value) {
        if (existingEnvVar) {
          existingEnvVar.value = this.value;
        } else {
          this.environmentVariables.push(ApplicationEnvironmentVariable.newValueType(this.key, this.value));
        }
        this.key = null;
        this.value = null;

      } else if (this.type === 'Secret' && this.secret && this.secretKey) {
        if (existingEnvVar) {
          existingEnvVar.secretName = this.secret.name;
          existingEnvVar.secretKey = this.secretKey;
        } else {
          this.environmentVariables.push(ApplicationEnvironmentVariable.newSecretType(this.key, this.secret.name, this.secretKey));
        }
        this.key = null;
        this.secret = null;
        this.secretKey = null;
      }
    }
  }

  removeEnvironmentVariable(environmentVariable: ApplicationEnvironmentVariable) {
    const index = this.environmentVariables.indexOf(environmentVariable);
    this.environmentVariables.splice(index, 1);
  }

  editEnvVarInit($event: any) {
    const index = $event.index;
    this.editingEnvVar = this.environmentVariables[index];
    if (this.editingEnvVar.type === 'Secret') {
      const secretFound = this.secrets.find(s => s.name === this.editingEnvVar.secretName);
      if (secretFound) {
        this.editingSecret = secretFound;
        this.editingSecretKey = this.editingEnvVar.secretKey;
        this.log.debug(`Initialized secret with name '${this.editingSecret.name}' and secret key '${this.editingSecretKey}' variables before inline editing`);
      }
    }
  }

  editEnvVarComplete($event: any) {
    if (this.editingEnvVar.type === 'Secret') {
      this.editingEnvVar.secretName = this.editingSecret.name;
      this.editingEnvVar.secretKey = this.editingSecretKey;
      this.log.debug('Updated secret name and key after inline editing', this.editingEnvVar);
    }

    const index = $event.index;
    this.environmentVariables[index] = this.editingEnvVar;
  }

  editingSecretChanged() {
    const allKeys = Object.keys(this.editingSecret.data);

    let firstKey = '';
    if (allKeys.length > 0) {
      firstKey = allKeys[0];
    }

    this.editingSecretKey = firstKey;
  }
}
