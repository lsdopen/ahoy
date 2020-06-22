import {Directive, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, Validator, ValidatorFn} from '@angular/forms';
import {Environment} from './environment';

@Directive({
  selector: '[appEnvironmentNameUnique]',
  providers: [{provide: NG_VALIDATORS, useExisting: EnvironmentNameUniqueValidatorDirective, multi: true}]
})
export class EnvironmentNameUniqueValidatorDirective implements Validator {
  @Input('appEnvironmentNameUnique') environments: Environment[];
  @Input() ignoreOwnId: number;

  constructor() {
  }

  validate(control: AbstractControl): { [key: string]: any } | null {
    return this.environments ? this.checkEnvironmentNameUnique(this.environments)(control) : null;
  }

  private checkEnvironmentNameUnique(environments: Environment[]): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const notUnique = environments
        .filter(rel => rel.id !== this.ignoreOwnId)
        .find(rel => rel.name === control.value);
      return notUnique ? {environmentNameNotUnique: {value: control.value}} : null;
    };
  }
}
