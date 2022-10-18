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

import {AfterViewInit, Component, EventEmitter, OnDestroy, OnInit, Renderer2} from '@angular/core';
import {PrimeNGConfig} from 'primeng/api';
import {AppearanceSettingsService} from 'projects/ahoy-components/src/public-api';
import {AppComponent} from './app.component';
import {MenuService} from './app.menu.service';

@Component({
  selector: 'app-main',
  templateUrl: './app.main.component.html'
})
export class AppMainComponent implements AfterViewInit, OnInit, OnDestroy {

  topbarMenuActive: boolean;

  menuActive: boolean;

  staticMenuDesktopInactive: boolean;

  mobileMenuActive: boolean;

  menuClick: boolean;

  mobileTopbarActive: boolean;

  topbarRightClick: boolean;

  topbarItemClick: boolean;

  activeTopbarItem: string;

  documentClickListener: () => void;

  configActive: boolean;

  configClick: boolean;

  rightMenuActive: boolean;

  menuHoverActive = false;

  searchClick = false;

  search = false;

  currentInlineMenuKey: string;

  inlineMenuActive: any[] = [];

  inlineMenuClick: boolean;

  public topBarMenuChanged = new EventEmitter<TopBarMenuClosedEvent>();

  constructor(public renderer: Renderer2,
              private menuService: MenuService,
              private primengConfig: PrimeNGConfig,
              public app: AppComponent,
              public appearanceSettingsService: AppearanceSettingsService) {
  }

  ngOnInit() {
    this.menuActive = this.isStatic() && !this.isMobile();
  }

  ngAfterViewInit() {
    // hides the horizontal submenus or top menu if outside is clicked
    this.documentClickListener = this.renderer.listen('body', 'click', () => {
      if (!this.topbarItemClick) {
        const topbarItem = this.activeTopbarItem;
        this.activeTopbarItem = null;
        this.topbarMenuActive = false;
        if (topbarItem) {
          this.topBarMenuChanged.emit(new TopBarMenuClosedEvent(topbarItem));
        }
      }

      if (!this.menuClick && (this.isHorizontal() || this.isSlim())) {
        this.menuService.reset();
      }

      if (this.configActive && !this.configClick) {
        this.configActive = false;
      }

      if (!this.menuClick) {
        if (this.mobileMenuActive) {
          this.mobileMenuActive = false;
        }

        if (this.isOverlay()) {
          this.menuActive = false;
        }

        this.menuHoverActive = false;
        this.unblockBodyScroll();
      }

      if (!this.searchClick) {
        this.search = false;
      }

      if (this.inlineMenuActive[this.currentInlineMenuKey] && !this.inlineMenuClick) {
        this.inlineMenuActive[this.currentInlineMenuKey] = false;
      }

      this.inlineMenuClick = false;
      this.searchClick = false;
      this.configClick = false;
      this.topbarItemClick = false;
      this.topbarRightClick = false;
      this.menuClick = false;
    });
  }

  onMenuButtonClick(event) {
    this.menuActive = !this.menuActive;
    this.topbarMenuActive = false;
    this.topbarRightClick = true;
    this.menuClick = true;

    if (this.isDesktop()) {
      this.staticMenuDesktopInactive = !this.staticMenuDesktopInactive;
    } else {
      this.mobileMenuActive = !this.mobileMenuActive;
      if (this.mobileMenuActive) {
        this.blockBodyScroll();
      } else {
        this.unblockBodyScroll();
      }
    }

    event.preventDefault();
  }

  onTopbarMobileButtonClick(event) {
    this.mobileTopbarActive = !this.mobileTopbarActive;
    event.preventDefault();
  }

  onRightMenuButtonClick(event) {
    this.rightMenuActive = !this.rightMenuActive;
    event.preventDefault();
  }

  onMenuClick($event) {
    this.menuClick = true;

    if (this.inlineMenuActive[this.currentInlineMenuKey] && !this.inlineMenuClick) {
      this.inlineMenuActive[this.currentInlineMenuKey] = false;
    }
  }

  onSearchKeydown(event) {
    if (event.keyCode === 27) {
      this.search = false;
    }
  }

  onInlineMenuClick(event, key) {
    if (key !== this.currentInlineMenuKey) {
      this.inlineMenuActive[this.currentInlineMenuKey] = false;
    }

    this.inlineMenuActive[key] = !this.inlineMenuActive[key];
    this.currentInlineMenuKey = key;
    this.inlineMenuClick = true;
  }

  onTopbarItemClick(event, item) {
    this.topbarItemClick = true;

    if (this.activeTopbarItem === item) {
      this.activeTopbarItem = null;
    } else {
      this.activeTopbarItem = item;
    }

    if (item === 'search') {
      this.search = !this.search;
      this.searchClick = !this.searchClick;
    }

    event.preventDefault();
  }

  onTopbarSubItemClick(event) {
    event.preventDefault();
  }

  onRTLChange(event) {
    this.app.themeService.isRTL = event.checked;
  }

  onRippleChange(event) {
    this.app.themeService.ripple = event.checked;
    this.primengConfig.ripple = event.checked;
  }

  onConfigClick(event) {
    this.configClick = true;
  }

  isDesktop() {
    return window.innerWidth > 991;
  }

  isMobile() {
    return window.innerWidth <= 991;
  }

  isOverlay() {
    return this.app.themeService.menuMode === 'overlay';
  }

  isStatic() {
    return this.app.themeService.menuMode === 'static';
  }

  isHorizontal() {
    return this.app.themeService.menuMode === 'horizontal';
  }

  isSlim() {
    return this.app.themeService.menuMode === 'slim';
  }

  blockBodyScroll(): void {
    if (document.body.classList) {
      document.body.classList.add('blocked-scroll');
    } else {
      document.body.className += ' blocked-scroll';
    }
  }

  unblockBodyScroll(): void {
    if (document.body.classList) {
      document.body.classList.remove('blocked-scroll');
    } else {
      document.body.className = document.body.className.replace(new RegExp('(^|\\b)' +
        'blocked-scroll'.split(' ').join('|') + '(\\b|$)', 'gi'), ' ');
    }
  }

  ngOnDestroy() {
    if (this.documentClickListener) {
      this.documentClickListener();
    }
  }
}

class TopBarMenuClosedEvent {
  item: string;

  constructor(item: string) {
    this.item = item;
  }
}
