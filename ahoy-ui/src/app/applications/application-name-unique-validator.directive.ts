import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Application} from './application';

@Directive({
  selector: '[appApplicationNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: ApplicationNameUniqueValidatorDirective, multi: true}]
})
export class ApplicationNameUniqueValidatorDirective implements Validator {
  @Input('appApplicationNameUnique') applications: Application[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.applications ? this.checkApplicationNameUnique(this.applications)(control) : null;
  }

  private checkApplicationNameUnique(applications: Application[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = applications
        .filter(app => app.id !== this.ignoreOwnId)
        .find(app => app.name === control.value);
      return notUnique ? {applicationNameNotUnique: {value: control.value}} : null;
    };
  }
}
