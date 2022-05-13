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
import {ApplicationSecret} from '../application';

@Component({
  selector: 'app-application-secrets',
  templateUrl: './application-secrets.component.html',
  styleUrls: ['./application-secrets.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationSecretsComponent {
  @Input() parentForm: NgForm;
  @Input() secret: ApplicationSecret;
  @Input() secretIndex: number;
  @Input() secretsForValidation: ApplicationSecret[];
  @Input() secretInUse: (secret) => boolean;

  constructor() {
  }

  secretAlreadyExists(secretIndex: number) {
    return this.parentForm.form.controls['secretName' + secretIndex]?.hasError('tabItemNameNotUnique');
  }
}
