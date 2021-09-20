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
  selector: '[appObjectNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ObjectNameUniqueValidatorDirective, multi: true}]
})
export class ObjectNameUniqueValidatorDirective implements Validator {
  @Input('appObjectNameUnique') objects: Nameable[];
  @Input() selectedIndex: number;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.objects ? this.checkObjectNameUnique(this.objects)(control) : null;
  }

  private checkObjectNameUnique(objects: Nameable[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {

      let notUnique = false;
      for (let i = 0; i < objects.length; i++) {
        if (i === this.selectedIndex || !control.value) {
          continue;
        }
        if (objects[i].name === control.value) {
          notUnique = true;
          break;
        }
      }

      return notUnique ? {objectNameNotUnique: {value: control.value}} : null;
    };
  }
}

export declare interface Nameable {
  name: string;
}
