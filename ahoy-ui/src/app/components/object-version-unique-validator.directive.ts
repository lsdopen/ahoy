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

import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';

@Directive({
  selector: '[appObjectVersionUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ObjectVersionUniqueValidatorDirective, multi: true}]
})
export class ObjectVersionUniqueValidatorDirective implements Validator {
  @Input('appObjectVersionUnique') objects: Versionable[];
  @Input() ignoreOwnId: number;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.objects ? this.checkObjectVersionUnique(this.objects)(control) : null;
  }

  private checkObjectVersionUnique(objects: Versionable[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = objects
        .filter(object => object.id !== this.ignoreOwnId)
        .find(object => object.version === control.value);
      return notUnique ? {objectVersionNotUnique: {value: control.value}} : null;
    };
  }
}

export declare interface Versionable {
  id: number;
  version: string;
}
