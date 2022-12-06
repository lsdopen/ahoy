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

import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Environment} from './environment';

@Directive({
  selector: '[appEnvironmentKeyUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: EnvironmentKeyUniqueValidatorDirective, multi: true}]
})
export class EnvironmentKeyUniqueValidatorDirective implements Validator {
  @Input('appEnvironmentKeyUnique') environments: Environment[];
  @Input() ignoreOwnId: number;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.environments ? this.checkEnvironmentKeyUnique(this.environments)(control) : null;
  }

  private checkEnvironmentKeyUnique(environments: Environment[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      if (control.value === null || control.value === undefined) {
        return null;
      }

      const notUnique = environments
        .filter(env => env.id !== this.ignoreOwnId)
        .find(env => env.key.toLowerCase() === control.value.toLowerCase());
      return notUnique ? {environmentKeyNotUnique: {value: control.value}} : null;
    };
  }
}
