import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {ReleaseVersion} from './release';

@Directive({
  selector: '[appReleaseVersionUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ReleaseVersionUniqueValidatorDirective, multi: true}]
})
export class ReleaseVersionUniqueValidatorDirective implements Validator {
  @Input('appReleaseVersionUnique') releaseVersions: ReleaseVersion[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.releaseVersions ? this.checkReleaseVersionUnique(this.releaseVersions)(control) : null;
  }

  private checkReleaseVersionUnique(releaseVersions: ReleaseVersion[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = releaseVersions
        .filter(rel => rel.id !== this.ignoreOwnId)
        .find(rel => rel.version === control.value);
      return notUnique ? {releaseVersionNotUnique: {value: control.value}} : null;
    };
  }
}
