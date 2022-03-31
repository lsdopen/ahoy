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

import {animate, state, style, transition, trigger} from '@angular/animations';
import {ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {filter} from 'rxjs/operators';
import {AppMainComponent} from './app.main.component';
import {MenuService} from './app.menu.service';

@Component({
  /* tslint:disable:component-selector */
  selector: '[app-menuitem]',
  /* tslint:enable:component-selector */
  template: `
    <ng-container>
      <div *ngIf="root">
        <span class="layout-menuitem-text">{{item.label}}</span>
      </div>
      <a [attr.href]="item.url" (click)="itemClick($event)" *ngIf="!item.routerLink || item.items" (keydown.enter)="itemClick($event)"
         [attr.target]="item.target" [attr.tabindex]="0" [ngClass]="item.class" (mouseenter)="onMouseEnter()" pRipple
         [pTooltip]="item.label" [tooltipDisabled]="active || !(root && app.isSlim() && !app.isMobile())">
        <i [ngClass]="item.icon" class="layout-menuitem-icon"></i>
        <span class="layout-menuitem-text">{{item.label}}</span>
        <span class="p-badge p-component p-badge-no-gutter" [ngClass]="item.badgeClass" *ngIf="item.badge && !root">{{item.badge}}</span>
        <i class="pi pi-fw pi-angle-down layout-submenu-toggler" *ngIf="item.items"></i>
      </a>
      <a (click)="itemClick($event)" *ngIf="item.routerLink && !item.items"
         [routerLink]="item.routerLink" routerLinkActive="active-menuitem-routerlink" [routerLinkActiveOptions]="{exact: true}"
         [attr.target]="item.target" [attr.tabindex]="0" [ngClass]="item.class" (mouseenter)="onMouseEnter()" pRipple
         [pTooltip]="item.label" [tooltipDisabled]="active || !(root && app.isSlim() && !app.isMobile())">
        <i [ngClass]="item.icon" class="layout-menuitem-icon"></i>
        <span class="layout-menuitem-text">{{item.label}}</span>
        <span class="p-badge p-component p-badge-no-gutter" [ngClass]="item.badgeClass" *ngIf="item.badge && !root">{{item.badge}}</span>
        <i class="pi pi-fw pi-angle-down layout-submenu-toggler" *ngIf="item.items"></i>
      </a>
      <ul *ngIf="(item.items && root) || (item.items && active)" [@children]="root ? 'visible' : active ? 'visibleAnimated' : 'hiddenAnimated'">
        <ng-template ngFor let-child let-i="index" [ngForOf]="item.items">
          <li app-menuitem [item]="child" [index]="i" [parentKey]="key" [class]="child.badgeClass" *appUserRole="child.roles"></li>
        </ng-template>
      </ul>
    </ng-container>
  `,
  host: {
    '[class.layout-root-menuitem]': 'root || active',
    '[class.active-menuitem]': '(active)'
  },
  animations: [
    trigger('children', [
      state('void', style({
        height: '0px',
        padding: '0px'
      })),
      state('hiddenAnimated', style({
        height: '0px',
        padding: '0px'
      })),
      state('visibleAnimated', style({
        height: '*'
      })),
      state('visible', style({
        height: '*'
      })),
      state('hidden', style({
        height: '0px',
        padding: '0px'
      })),
      transition('visibleAnimated => hiddenAnimated', animate('400ms cubic-bezier(0.86, 0, 0.07, 1)')),
      transition('hiddenAnimated => visibleAnimated', animate('400ms cubic-bezier(0.86, 0, 0.07, 1)')),
      transition('void => visibleAnimated, visibleAnimated => void',
        animate('400ms cubic-bezier(0.86, 0, 0.07, 1)'))
    ])
  ]
})
export class AppMenuitemComponent implements OnInit, OnDestroy {

  @Input() item: any;

  @Input() index: number;

  @Input() root: boolean;

  @Input() parentKey: string;

  active = false;

  menuSourceSubscription: Subscription;

  menuResetSubscription: Subscription;

  key: string;

  constructor(public app: AppMainComponent, public router: Router, private cd: ChangeDetectorRef, private menuService: MenuService) {
    this.menuSourceSubscription = this.menuService.menuSource$.subscribe(key => {
      // deactivate current active menu
      if (this.active && this.key !== key && key.indexOf(this.key) !== 0) {
        this.active = false;
      }
    });

    this.menuResetSubscription = this.menuService.resetSource$.subscribe(() => {
      this.active = false;
    });

    this.router.events.pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(params => {
        if (this.app.isHorizontal() || this.app.isSlim()) {
          this.active = false;
        } else {
          if (this.item.routerLink) {
            this.updateActiveStateFromRoute();
          } else {
            this.active = false;
          }
        }
      });
  }

  ngOnInit() {
    if (!(this.app.isHorizontal() || this.app.isSlim()) && this.item.routerLink) {
      this.updateActiveStateFromRoute();
    }

    this.key = this.parentKey ? this.parentKey + '-' + this.index : String(this.index);
  }

  updateActiveStateFromRoute() {
    this.active = this.router.isActive(this.item.routerLink[0], this.item.items ? false : true);
  }

  itemClick(event: Event) {
    // avoid processing disabled items
    if (this.item.disabled) {
      event.preventDefault();
      return true;
    }

    // navigate with hover in horizontal mode
    if (this.root) {
      this.app.menuHoverActive = !this.app.menuHoverActive;
    }

    // notify other items
    this.menuService.onMenuStateChange(this.key);

    // execute command
    if (this.item.command) {
      this.item.command({originalEvent: event, item: this.item});
    }

    // toggle active state
    if (this.item.items) {
      this.active = !this.active;
    } else {
      // activate item
      this.active = true;

      // reset horizontal and slim menu
      if (this.app.isHorizontal() || this.app.isSlim()) {
        this.menuService.reset();
        this.app.menuHoverActive = false;
      }

      if (!this.app.isStatic()) {
        this.app.menuActive = false;
      }

      this.app.mobileMenuActive = false;
    }

    this.removeActiveInk(event);
  }

  onMouseEnter() {
    // activate item on hover
    if (this.root && (this.app.isHorizontal() || this.app.isSlim()) && this.app.isDesktop()) {
      if (this.app.menuHoverActive) {
        this.menuService.onMenuStateChange(this.key);
        this.active = true;
      }
    }
  }

  removeActiveInk(event: Event) {
    let currentTarget = (event.currentTarget as HTMLElement);
    setTimeout(() => {
      if (currentTarget) {
        let activeInk = currentTarget.querySelector('.p-ink-active');
        if (activeInk) {
          if (activeInk.classList)
            activeInk.classList.remove('p-ink-active');
          else
            activeInk.className = activeInk.className.replace(new RegExp('(^|\\b)' + 'p-ink-active'.split(' ').join('|') + '(\\b|$)', 'gi'), ' ');
        }
      }
    }, 401);
  }

  ngOnDestroy() {
    if (this.menuSourceSubscription) {
      this.menuSourceSubscription.unsubscribe();
    }

    if (this.menuResetSubscription) {
      this.menuResetSubscription.unsubscribe();
    }
  }
}
