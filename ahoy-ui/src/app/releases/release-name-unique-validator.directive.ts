import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Release} from './release';

@Directive({
  selector: '[appReleaseNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ReleaseNameUniqueValidatorDirective, multi: true}]
})
export class ReleaseNameUniqueValidatorDirective implements Validator {
  @Input('appReleaseNameUnique') releases: Release[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.releases ? this.checkReleaseNameUnique(this.releases)(control) : null;
  }

  private checkReleaseNameUnique(releases: Release[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = releases
        .filter(rel => rel.id !== this.ignoreOwnId)
        .find(rel => rel.name === control.value);
      return notUnique ? {releaseNameNotUnique: {value: control.value}} : null;
    };
  }
}
