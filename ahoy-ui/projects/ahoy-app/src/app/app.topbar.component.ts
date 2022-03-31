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

import {animate, AnimationEvent, style, transition, trigger} from '@angular/animations';
import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AppearanceSettingsService, AuthService, Role, ServerService, TaskEvent} from 'projects/ahoy-components/src/public-api';
import {AppComponent} from './app.component';
import {AppMainComponent} from './app.main.component';

@Component({
  selector: 'app-topbar',
  templateUrl: './app.topbar.component.html',
  animations: [
    trigger('topbarActionPanelAnimation', [
      transition(':enter', [
        style({opacity: 0, transform: 'scaleY(0.8)'}),
        animate('.12s cubic-bezier(0, 0, 0.2, 1)', style({opacity: 1, transform: '*'})),
      ]),
      transition(':leave', [
        animate('.1s linear', style({opacity: 0}))
      ])
    ])
  ]
})
export class AppTopBarComponent implements OnInit {
  Role = Role;
  argoConnected: boolean;
  @ViewChild('searchInput') searchInputViewChild: ElementRef;

  constructor(public appMain: AppMainComponent, public app: AppComponent,
              private authService: AuthService,
              private serverService: ServerService,
              private appearanceSettingsService: AppearanceSettingsService) {
  }

  ngOnInit(): void {
    this.serverService.getServerStatus().subscribe((serverStatus) => {
      this.argoConnected = serverStatus.argoCdConnected;
    });
  }

  onSearchAnimationEnd(event: AnimationEvent) {
    switch (event.toState) {
      case 'visible':
        this.searchInputViewChild.nativeElement.focus();
        break;
    }
  }

  identityClaim() {
    return this.authService.identityClaim();
  }

  userInitials(): string {
    return this.authService.userInitials();
  }

  accountUri(): string {
    return this.authService.accountUri();
  }

  logout() {
    this.authService.logout();
  }

  appLogoMode() {
    const topbarTheme = this.appearanceSettingsService.appearanceSettings.topbarTheme;
    if (topbarTheme === 'white' || topbarTheme === 'yellow' || topbarTheme === 'amber' || topbarTheme === 'orange' || topbarTheme === 'lime') {
      return 'dark';
    } else {
      return 'light';
    }
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.argoConnectionEvent) {
      this.argoConnected = event.argoConnectionEvent.connected;
    }
  }
}
