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

import {Component} from '@angular/core';
import {AppearanceSettingsService} from 'projects/ahoy-components/src/public-api';
import packageJson from '../../../../package.json';
import {AppComponent} from './app.component';

@Component({
  selector: 'app-footer',
  template: `
    <div class="layout-footer flex align-items-center p-4 shadow-2">
      <img id="footer-logo" [src]="'assets/layout/images/footer-' + (appearanceSettingsService.appearanceSettings.mode === 'light' ? 'dark' : 'light') + '.png'" alt="ultima-footer-logo" style="height: 2.25rem">
      <span [ngClass]="{'ml-auto mr-2': !app.themeService.isRTL, 'ml-2 mr-auto': app.themeService.isRTL}">{{version}}</span>
      <a pButton pRipple icon="pi pi-github fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://github.com/lsdopen" target="_blank"
         [ngClass]="{'mr-2': !app.themeService.isRTL, 'ml-2': app.themeService.isRTL}"></a>
      <a pButton pRipple type="button" icon="pi pi-facebook fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://www.facebook.com/lsdopen" target="_blank"
         [ngClass]="{'mr-2': !app.themeService.isRTL, 'ml-2': app.themeService.isRTL}"></a>
      <a pButton pRipple type="button" icon="pi pi-twitter fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://twitter.com/lsdopen" target="_blank"
         [ngClass]="{'mr-2': !app.themeService.isRTL, 'ml-2': app.themeService.isRTL}"></a>
    </div>
  `
})
export class AppFooterComponent {
  public version = packageJson.version;

  constructor(public app: AppComponent,
              public appearanceSettingsService: AppearanceSettingsService) {
  }
}
