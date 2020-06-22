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

import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-application-env-variables',
  templateUrl: './application-env-variables.component.html',
  styleUrls: ['./application-env-variables.component.scss']
})
export class ApplicationEnvVariablesComponent implements OnInit {
  @Input() environmentVariables: { [key: string]: string };
  private newEnvironmentVariableKey: string;
  private newEnvironmentVariableValue: string;
  data: EnvironmentVariable[];
  displayedColumns = ['key', 'value', 'remove'];

  constructor() {
  }

  ngOnInit() {
    this.refresh();
  }

  addEnvironmentVariable() {
    if (this.newEnvironmentVariableKey && this.newEnvironmentVariableValue) {
      this.environmentVariables[this.newEnvironmentVariableKey] = this.newEnvironmentVariableValue;
      this.refresh();
      this.newEnvironmentVariableKey = null;
      this.newEnvironmentVariableValue = null;
    }
  }

  removeEnvironmentVariable(key: string) {
    delete this.environmentVariables[key];
    this.refresh();
  }

  refresh() {
    this.data = [];
    if (this.environmentVariables) {
      for (const key of Object.keys(this.environmentVariables)) {
        this.data.push(new EnvironmentVariable(key, this.environmentVariables[key]));
      }
    }
  }
}

export class EnvironmentVariable {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}
