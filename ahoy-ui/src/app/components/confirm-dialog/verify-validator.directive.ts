import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';

@Directive({
  selector: '[appVerifyText]',
  providers: [{provide: NG_VALIDATORS, useExisting: VerifyValidatorDirective, multi: true}]
})
export class VerifyValidatorDirective implements Validator {
  @Input('appVerifyText') verifyText: string;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.verifyText ? this.checkTextEqual(this.verifyText)(control) : null;
  }

  private checkTextEqual(verifyText: string): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      return verifyText !== control.value ? {verifyText: {value: control.value}} : null;
    };
  }
}
