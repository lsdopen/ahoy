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

import {AfterContentChecked, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {ControlContainer, NgForm} from '@angular/forms';
import {ApplicationSecret, ApplicationVolume} from '../application';

@Component({
  selector: 'app-application-volumes',
  templateUrl: './application-volumes.component.html',
  styleUrls: ['./application-volumes.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationVolumesComponent implements OnInit, AfterContentChecked {
  @Input() volumes: ApplicationVolume[];
  @Input() secrets: ApplicationSecret[];
  selectedVolumeIndex = 0;

  constructor(private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  addVolume() {
    this.volumes.push(new ApplicationVolume());
    setTimeout(() => this.selectedVolumeIndex = this.volumes.length - 1);
  }

  deleteVolume() {
    this.volumes.splice(this.selectedVolumeIndex, 1);
    setTimeout(() => {
      if (this.selectedVolumeIndex === this.volumes.length) {
        // only move one tab back if its the last tab
        this.selectedVolumeIndex = this.volumes.length - 1;
      }
    });
  }
}
