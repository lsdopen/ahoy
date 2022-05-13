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
import {ThemeService} from '../../util/theme.service';
import {AppearanceSettingsService} from './appearance-settings.service';

@Component({
  selector: 'app-appearance-settings',
  templateUrl: './appearance-settings.component.html',
  styleUrls: ['./appearance-settings.component.scss']
})
export class AppearanceSettingsComponent implements OnInit {
  scale: number;
  scales: number[] = [12, 13, 14, 15, 16];

  themes: any[];
  menuThemes: any[];
  topbarThemes: any[];

  selectedMenuTheme: any;
  selectedTopbarTheme: any;

  constructor(public themeService: ThemeService,
              public appearanceSettingsService: AppearanceSettingsService) {
  }

  ngOnInit() {
    this.themes = [
      {name: 'indigo', color: '#2f8ee5'},
      {name: 'pink', color: '#E91E63'},
      {name: 'purple', color: '#9C27B0'},
      {name: 'deeppurple', color: '#673AB7'},
      {name: 'blue', color: '#2196F3'},
      {name: 'lightblue', color: '#03A9F4'},
      {name: 'cyan', color: '#00BCD4'},
      {name: 'teal', color: '#009688'},
      {name: 'green', color: '#4CAF50'},
      {name: 'lightgreen', color: '#8BC34A'},
      {name: 'lime', color: '#CDDC39'},
      {name: 'yellow', color: '#FFEB3B'},
      {name: 'amber', color: '#FFC107'},
      {name: 'orange', color: '#FF9800'},
      {name: 'deeporange', color: '#FF5722'},
      {name: 'brown', color: '#795548'},
      {name: 'bluegrey', color: '#607D8B'}
    ];

    this.menuThemes = [
      {name: 'light', color: '#FDFEFF'},
      {name: 'dark', color: '#434B54'},
      {name: 'indigo', color: '#1A237E'},
      {name: 'bluegrey', color: '#37474F'},
      {name: 'brown', color: '#4E342E'},
      {name: 'cyan', color: '#006064'},
      {name: 'green', color: '#2E7D32'},
      {name: 'deeppurple', color: '#4527A0'},
      {name: 'deeporange', color: '#BF360C'},
      {name: 'pink', color: '#880E4F'},
      {name: 'purple', color: '#6A1B9A'},
      {name: 'teal', color: '#00695C'}
    ];

    this.topbarThemes = [
      {name: 'lightblue', color: '#2E88FF'},
      {name: 'dark', color: '#363636'},
      {name: 'white', color: '#FDFEFF'},
      {name: 'blue', color: '#1565C0'},
      {name: 'deeppurple', color: '#4527A0'},
      {name: 'purple', color: '#6A1B9A'},
      {name: 'pink', color: '#AD1457'},
      {name: 'cyan', color: '#0097A7'},
      {name: 'teal', color: '#00796B'},
      {name: 'green', color: '#43A047'},
      {name: 'lightgreen', color: '#689F38'},
      {name: 'lime', color: '#AFB42B'},
      {name: 'yellow', color: '#FBC02D'},
      {name: 'amber', color: '#FFA000'},
      {name: 'orange', color: '#FB8C00'},
      {name: 'deeporange', color: '#D84315'},
      {name: 'brown', color: '#5D4037'},
      {name: 'grey', color: '#616161'},
      {name: 'bluegrey', color: '#546E7A'},
      {name: 'indigo', color: '#3F51B5'}
    ];

    this.selectedMenuTheme = this.menuThemes.find(theme => theme.name === this.appearanceSettingsService.appearanceSettings.menuTheme);
    this.selectedTopbarTheme = this.topbarThemes.find(theme => theme.name === this.appearanceSettingsService.appearanceSettings.topbarTheme);
    this.scale = this.appearanceSettingsService.appearanceSettings.scale;
  }

  decrementScale() {
    this.scale--;
    this.appearanceSettingsService.changeScale(this.scale);
  }

  incrementScale() {
    this.scale++;
    this.appearanceSettingsService.changeScale(this.scale);
  }

  onLayoutModeChange(event, mode) {
    this.appearanceSettingsService.changeMode(mode);
  }

  changeTheme(theme) {
    this.appearanceSettingsService.changeTheme(theme);
  }

  changeMenuTheme(theme) {
    this.selectedMenuTheme = theme;
    this.appearanceSettingsService.changeMenuTheme(theme.name);
  }

  changeTopbarTheme(theme) {
    this.selectedTopbarTheme = theme;
    this.appearanceSettingsService.changeTopbarTheme(theme.name);
  }

  save() {
    this.appearanceSettingsService.save();
  }

  default() {
    this.appearanceSettingsService.default();
  }
}
