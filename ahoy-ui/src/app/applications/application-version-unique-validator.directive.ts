import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {ApplicationVersion} from './application';

@Directive({
  selector: '[appApplicationVersionUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ApplicationVersionUniqueValidatorDirective, multi: true}]
})
export class ApplicationVersionUniqueValidatorDirective implements Validator {
  @Input('appApplicationVersionUnique') applicationVersions: ApplicationVersion[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.applicationVersions ? this.checkApplicationVersionUnique(this.applicationVersions)(control) : null;
  }

  private checkApplicationVersionUnique(applicationVersions: ApplicationVersion[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = applicationVersions
        .filter(rel => rel.id !== this.ignoreOwnId)
        .find(rel => rel.version === control.value);
      return notUnique ? {applicationVersionNotUnique: {value: control.value}} : null;
    };
  }
}
