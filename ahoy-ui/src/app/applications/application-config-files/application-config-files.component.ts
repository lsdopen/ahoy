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

import {Component, Input, OnInit} from '@angular/core';
import {ApplicationConfig, ApplicationVersion} from '../application';
import {ControlContainer, NgForm} from '@angular/forms';

@Component({
  selector: 'app-application-config-files',
  templateUrl: './application-config-files.component.html',
  styleUrls: ['./application-config-files.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationConfigFilesComponent implements OnInit {
  @Input() applicationVersion: ApplicationVersion;
  @Input() configs: ApplicationConfig[];
  @Input() editPath = true;
  selectedConfigIndex = 0;

  constructor() {
  }

  ngOnInit(): void {
  }

  addConfig() {
    this.configs.push(new ApplicationConfig());
    setTimeout(() => this.selectedConfigIndex = this.configs.length - 1, 300);
  }

  deleteConfig() {
    const indexToRemove = this.selectedConfigIndex;
    this.selectedConfigIndex = 0;
    this.configs.splice(indexToRemove, 1);
  }
}
