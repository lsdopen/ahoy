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
  selector: '[appVerifyText]',
  providers: [{provide: NG_VALIDATORS, useExisting: VerifyValidatorDirective, multi: true}]
})
export class VerifyValidatorDirective implements Validator {
  @Input('appVerifyText') verifyText: string;

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.verifyText ? this.checkTextEqual(this.verifyText)(control) : null;
  }

  private checkTextEqual(verifyText: string): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      return verifyText !== control.value ? {verifyText: {value: control.value}} : null;
    };
  }
}
