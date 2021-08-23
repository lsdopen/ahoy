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
import {ApplicationSecret, ApplicationVolume} from '../application';
import {ControlContainer, NgForm} from '@angular/forms';

@Component({
  selector: 'app-application-volumes',
  templateUrl: './application-volumes.component.html',
  styleUrls: ['./application-volumes.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationVolumesComponent implements OnInit {
  @Input() volumes: ApplicationVolume[];
  @Input() secrets: ApplicationSecret[];
  selectedVolumeIndex = 0;

  constructor() { }

  ngOnInit(): void {
  }

  addVolume() {
    this.volumes.push(new ApplicationVolume());
    setTimeout(() => this.selectedVolumeIndex = this.volumes.length - 1, 300);
  }

  deleteVolume() {
    const indexToRemove = this.selectedVolumeIndex;
    this.selectedVolumeIndex = 0;
    this.volumes.splice(indexToRemove, 1);
  }
}
