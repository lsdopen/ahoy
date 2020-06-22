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

import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Environment} from './environment';

@Directive({
  selector: '[appEnvironmentNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: EnvironmentNameUniqueValidatorDirective, multi: true}]
})
export class EnvironmentNameUniqueValidatorDirective implements Validator {
  @Input('appEnvironmentNameUnique') environments: Environment[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.environments ? this.checkEnvironmentNameUnique(this.environments)(control) : null;
  }

  private checkEnvironmentNameUnique(environments: Environment[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = environments
        .filter(rel => rel.id !== this.ignoreOwnId)
        .find(rel => rel.name === control.value);
      return notUnique ? {environmentNameNotUnique: {value: control.value}} : null;
    };
  }
}
