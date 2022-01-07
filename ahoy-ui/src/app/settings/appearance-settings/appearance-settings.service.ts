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

import {Injectable} from '@angular/core';
import {LocalStorageService} from '../../util/local-storage.service';
import {AppearanceSettings} from './appearance-settings';

@Injectable({
  providedIn: 'root'
})
export class AppearanceSettingsService {
  private readonly KEY = 'appearance-settings';
  public appearanceSettings: AppearanceSettings;

  constructor(private localStorageService: LocalStorageService) {

    const item = this.localStorageService.getItem(this.KEY);
    if (item) {
      this.appearanceSettings = JSON.parse(item);
    } else {
      this.appearanceSettings = new AppearanceSettings();
    }

    this.updateLinks();
  }

  public changeMode(mode: string) {
    this.appearanceSettings.mode = mode;

    if (mode === 'dark') {
      this.appearanceSettings.menuTheme = 'dark';
      this.appearanceSettings.topbarTheme = 'dark';
    } else {
      this.appearanceSettings.menuTheme = 'light';
      this.appearanceSettings.topbarTheme = 'bluegrey';
    }

    this.updateLinks();
  }

  public changeTheme(theme: string) {
    this.appearanceSettings.theme = theme;
    this.updateLinks();
  }

  public changeMenuTheme(menuTheme: string) {
    this.appearanceSettings.menuTheme = menuTheme;
  }

  public changeTopbarTheme(topbarTheme: string) {
    this.appearanceSettings.topbarTheme = topbarTheme;
  }

  public changeScale(scale: number) {
    this.appearanceSettings.scale = scale;
    this.updateLinks();
  }

  public save(): void {
    this.localStorageService.setItem(this.KEY, JSON.stringify(this.appearanceSettings));
  }

  public default() {
    this.appearanceSettings = new AppearanceSettings();
    this.updateLinks();
  }

  private updateLinks(): void {
    const layoutLink: HTMLLinkElement = document.getElementById('layout-css') as HTMLLinkElement;
    const layoutHref = 'assets/layout/css/layout-' + this.appearanceSettings.mode + '.css';
    this.replaceLink(layoutLink, layoutHref);

    const themeLink: HTMLLinkElement = document.getElementById('theme-css') as HTMLLinkElement;
    const themeHref = 'assets/theme/' + this.appearanceSettings.theme + '/theme-' + this.appearanceSettings.mode + '.css';
    this.replaceLink(themeLink, themeHref);

    // apply scale
    document.documentElement.style.fontSize = this.appearanceSettings.scale + 'px';
  }

  private isIE() {
    return /(MSIE|Trident\/|Edge\/)/i.test(window.navigator.userAgent);
  }

  private replaceLink(linkElement, href, callback?) {
    if (!linkElement) {
      return;
    }
    if (this.isIE()) {
      linkElement.setAttribute('href', href);
      if (callback) {
        callback();
      }
    } else {
      const id = linkElement.getAttribute('id');
      const cloneLinkElement = linkElement.cloneNode(true);

      cloneLinkElement.setAttribute('href', href);
      cloneLinkElement.setAttribute('id', id + '-clone');

      linkElement.parentNode.insertBefore(cloneLinkElement, linkElement.nextSibling);

      cloneLinkElement.addEventListener('load', () => {
        linkElement.remove();
        cloneLinkElement.setAttribute('id', id);

        if (callback) {
          callback();
        }
      });
    }
  }
}
