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

@Directive({
  selector: '[appVersionUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: VersionUniqueValidatorDirective, multi: true}]
})
export class VersionUniqueValidatorDirective implements Validator {
  @Input('appVersionUnique') versionables: Versionable[];
  @Input() ignoreOwnId: number;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.versionables ? this.checkVersionUnique(this.versionables)(control) : null;
  }

  private checkVersionUnique(versionables: Versionable[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = versionables
        .filter(versionable => versionable.id !== this.ignoreOwnId)
        .find(versionable => versionable.version === control.value);
      return notUnique ? {versionNotUnique: {value: control.value}} : null;
    };
  }
}

export declare interface Versionable {
  id: number;
  version: string;
}
