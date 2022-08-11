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
import {ApplicationSpec, ContainerSpec} from '../application';

@Component({
  selector: 'app-container-detail',
  templateUrl: './container-detail.component.html',
  styleUrls: ['./container-detail.component.css'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ContainerDetailComponent {
  @Input() parentForm: NgForm;
  @Input() applicationSpec: ApplicationSpec;
  @Input() containerSpec: ContainerSpec;
  @Input() containerSpecIndex: number;
  @Input() defaultContainerSpec: boolean;
  @Input() containerSpecsForValidation: ContainerSpec[];
  newArg: string;
  editingArg: string;
  newServicePort: number;
  editingPort: number;

  addArg() {
    if (this.newArg) {
      if (!this.containerSpec.args) {
        this.containerSpec.args = [];
      }
      this.containerSpec.args.push(this.newArg);
      this.newArg = null;
    }
  }

  removeArg(argIndex: number) {
    this.containerSpec.args.splice(argIndex, 1);
  }

  editArgInit($event: any) {
    const index = $event.index;
    this.editingArg = this.containerSpec.args[index];
  }

  editArgComplete($event: any) {
    const index = $event.index;
    this.containerSpec.args[index] = this.editingArg;
  }

  addServicePort() {
    if (this.newServicePort) {
      this.containerSpec.servicePorts.push(this.newServicePort);
      this.newServicePort = null;
    }
  }

  removeServicePort(portIndex: number) {
    this.containerSpec.servicePorts.splice(portIndex, 1);
  }

  editPortInit($event: any) {
    const index = $event.index;
    this.editingPort = this.containerSpec.servicePorts[index];
  }

  editPortComplete($event: any) {
    const index = $event.index;
    this.containerSpec.servicePorts[index] = this.editingPort;
  }
}
