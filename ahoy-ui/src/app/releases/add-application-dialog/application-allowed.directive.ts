import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Application} from '../../applications/application';


@Directive({
  selector: '[appApplicationAllowed]',
  providers: [{provide: NG_VALIDATORS, useExisting: ApplicationAllowedValidatorDirective, multi: true}]
})
export class ApplicationAllowedValidatorDirective implements Validator {
  @Input('appApplicationAllowed') applications: Application[];

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.applications ? this.checkApplicationAllowed(this.applications)(control) : null;
  }

  private checkApplicationAllowed(applications: Application[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const selectedApp: Application = control.value;
      if (selectedApp) {
        const notAllowed = applications
          .find(app => app.name === selectedApp.name);
        return notAllowed ? {applicationNameNotUnique: {value: control.value}} : null;
      } else {
        return null;
      }
    };
  }
}
