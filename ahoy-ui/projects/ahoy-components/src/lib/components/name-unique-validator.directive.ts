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
  selector: '[appNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: NameUniqueValidatorDirective, multi: true}]
})
export class NameUniqueValidatorDirective implements Validator {
  @Input('appNameUnique') nameables: Nameable[];
  @Input() ignoreOwnId: number;
  @Input() ignoreSelf: Nameable;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.nameables ? this.checkNameUnique(this.nameables)(control) : null;
  }

  private checkNameUnique(nameables: Nameable[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = nameables
        .filter(nameable => this.ignoreOwnId ? nameable.id !== this.ignoreOwnId : true)
        .filter(nameable => this.ignoreSelf ? nameable !== this.ignoreSelf : true)
        .find(nameable => nameable.name === control.value);
      return notUnique ? {nameNotUnique: {value: control.value}} : null;
    };
  }
}

export declare interface Nameable {
  id?: number;
  name: string;
}
