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

@Component({
  selector: 'app-application-secret-data',
  templateUrl: './application-secret-data.component.html',
  styleUrls: ['./application-secret-data.component.scss']
})
export class ApplicationSecretDataComponent implements OnInit {
  @Input() secretData: { [key: string]: string };
  @Input() secretIndex: number;
  newDataKey: string;
  newDataValue: string;
  data: SecretData[];
  displayedColumns = ['key', 'value', 'remove'];
  hideValue = true;

  ngOnInit() {
    this.refresh();
  }

  addSecretData() {
    if (this.newDataKey && this.newDataValue) {
      this.secretData[this.newDataKey] = this.newDataValue;
      this.refresh();
      this.newDataKey = null;
      this.newDataValue = null;
    }
  }

  removeSecretData(key: string) {
    delete this.secretData[key];
    this.refresh();
  }

  refresh() {
    this.data = [];
    if (this.secretData) {
      for (const key of Object.keys(this.secretData)) {
        this.data.push(new SecretData(key, this.secretData[key]));
      }
    }
  }
}

export class SecretData {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}
