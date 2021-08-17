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

import {Component, ElementRef, ViewChild} from '@angular/core';
import {animate, AnimationEvent, style, transition, trigger} from '@angular/animations';
import {AppComponent} from './app.component';
import {AppMainComponent} from './app.main.component';
import {AuthService} from './util/auth.service';

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
export class AppTopBarComponent {

  constructor(public appMain: AppMainComponent, public app: AppComponent,
              private authService: AuthService) {
  }

  @ViewChild('searchInput') searchInputViewChild: ElementRef;

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

  issuer(): string {
    return this.authService.issuer();
  }

  logout() {
    this.authService.logout();
  }
}
