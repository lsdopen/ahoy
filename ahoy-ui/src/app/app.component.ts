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

import {Component, OnInit} from '@angular/core';
import {PrimeNGConfig} from 'primeng/api';
import {AppearanceSettingsService} from './settings/appearance-settings/appearance-settings.service';
import {AuthService} from './util/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'Ahoy';

  // theme
  menuMode = 'static';
  inlineMenuPosition = 'top';
  inputStyle = 'filled';
  ripple = true;
  isRTL = false;

  constructor(private primengConfig: PrimeNGConfig,
              public authService: AuthService,
              public appearanceSettingsService: AppearanceSettingsService) {
  }

  ngOnInit() {
    this.primengConfig.ripple = true;
  }
}
