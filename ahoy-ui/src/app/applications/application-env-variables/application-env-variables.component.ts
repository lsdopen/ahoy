import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-application-env-variables',
  templateUrl: './application-env-variables.component.html',
  styleUrls: ['./application-env-variables.component.scss']
})
export class ApplicationEnvVariablesComponent implements OnInit {
  @Input() environmentVariables: { [key: string]: string };
  private newEnvironmentVariableKey: string;
  private newEnvironmentVariableValue: string;
  data: EnvironmentVariable[];
  displayedColumns = ['key', 'value', 'remove'];

  constructor() {
  }

  ngOnInit() {
    this.refresh();
  }

  addEnvironmentVariable() {
    if (this.newEnvironmentVariableKey && this.newEnvironmentVariableValue) {
      this.environmentVariables[this.newEnvironmentVariableKey] = this.newEnvironmentVariableValue;
      this.refresh();
      this.newEnvironmentVariableKey = null;
      this.newEnvironmentVariableValue = null;
    }
  }

  removeEnvironmentVariable(key: string) {
    delete this.environmentVariables[key];
    this.refresh();
  }

  refresh() {
    this.data = [];
    if (this.environmentVariables) {
      for (const key of Object.keys(this.environmentVariables)) {
        this.data.push(new EnvironmentVariable(key, this.environmentVariables[key]));
      }
    }
  }
}

export class EnvironmentVariable {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}
