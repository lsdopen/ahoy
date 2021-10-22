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

import {AfterContentChecked, ChangeDetectorRef, Component, Input} from '@angular/core';
import {ControlContainer, NgForm} from '@angular/forms';
import {TabItemFactory} from '../../components/multi-tab/multi-tab.component';
import {ApplicationConfigFile, ApplicationVersion} from '../application';

@Component({
  selector: 'app-application-config-files',
  templateUrl: './application-config-files.component.html',
  styleUrls: ['./application-config-files.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationConfigFilesComponent implements AfterContentChecked {
  @Input() parentForm: NgForm;
  @Input() applicationVersion: ApplicationVersion;
  @Input() configFiles: ApplicationConfigFile[];
  @Input() editPath = true;

  constructor(private cd: ChangeDetectorRef) {
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  applicationConfigFileFactory(): TabItemFactory<ApplicationConfigFile> {
    return (): ApplicationConfigFile => {
      return new ApplicationConfigFile();
    };
  }
}
