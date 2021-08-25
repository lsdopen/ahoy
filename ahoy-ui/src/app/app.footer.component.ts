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

import {Component} from '@angular/core';
import {version} from '../../package.json';
import {AppComponent} from './app.component';

@Component({
  selector: 'app-footer',
  template: `
    <div class="layout-footer p-d-flex p-ai-center p-p-4 p-shadow-2">
      <img id="footer-logo" [src]="'assets/layout/images/footer-' + (app.layoutMode === 'light' ? 'dark' : 'light') + '.png'" alt="ultima-footer-logo" style="height: 2.25rem">
      <span [ngClass]="{'p-ml-auto p-mr-2': !app.isRTL, 'p-ml-2 p-mr-auto': app.isRTL}">{{version}}</span>
      <a pButton pRipple icon="pi pi-github fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://github.com/lsdopen" target="_blank"
         [ngClass]="{'p-mr-2': !app.isRTL, 'p-ml-2': app.isRTL}"></a>
      <a pButton pRipple type="button" icon="pi pi-facebook fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://www.facebook.com/lsdopen" target="_blank"
         [ngClass]="{'p-mr-2': !app.isRTL, 'p-ml-2': app.isRTL}"></a>
      <a pButton pRipple type="button" icon="pi pi-twitter fs-large" class="p-button-rounded p-button-text p-button-plain"
         href="https://twitter.com/lsdopen" target="_blank"
         [ngClass]="{'p-mr-2': !app.isRTL, 'p-ml-2': app.isRTL}"></a>
    </div>
  `
})
export class AppFooterComponent {
  public version: string = version;

  constructor(public app: AppComponent) {
  }
}
