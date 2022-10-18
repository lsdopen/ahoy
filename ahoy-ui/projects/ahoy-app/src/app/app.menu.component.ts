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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {RecentRelease, RecentReleasesService, Role} from 'projects/ahoy-components/src/public-api';
import {Subscription} from 'rxjs';
import {AppComponent} from './app.component';

@Component({
  selector: 'app-menu',
  template: `
    <ul class="layout-menu">
      <li app-menuitem *ngFor="let item of model; let i = index;" [item]="item" [index]="i" [root]="true"></li>
    </ul>
  `
})
export class AppMenuComponent implements OnInit, OnDestroy {

  model: any[];
  private recentReleasesChangedSubscription: Subscription;

  constructor(public app: AppComponent,
              private recentReleasesService: RecentReleasesService) {
  }

  ngOnInit() {
    this.model = [
      {
        label: 'Favorites', icon: 'pi pi-fw pi-home',
        items: [
          {label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'], roles: [Role.user]},
        ]
      },
      {
        label: 'Recent'
      },
      {
        label: 'Manage', icon: 'pi pi-fw pi-star', routerLink: ['/manage'],
        items: [
          {label: 'Releases', icon: 'pi pi-fw pi-forward', routerLink: ['/releases'], roles: [Role.user]},
          {label: 'Environments', icon: 'pi pi-fw pi-folder', routerLink: ['/environments'], roles: [Role.admin, Role.releasemanager]},
          {label: 'Applications', icon: 'pi pi-fw pi-image', routerLink: ['/applications'], roles: [Role.admin, Role.releasemanager, Role.developer]},
          {label: 'Clusters', icon: 'pi pi-fw pi-table', routerLink: ['/clusters'], roles: [Role.admin]},
        ]
      }
    ];
    this.recentReleasesChanged(this.recentReleasesService.getRecentReleases());
    this.recentReleasesChangedSubscription = this.recentReleasesService.recentReleasesChanged()
      .subscribe((recentReleases) => {
        // give the button animation some time before reloading the recent releases
        setTimeout(() => {
          this.recentReleasesChanged(recentReleases);
        }, 500);
      });
  }

  ngOnDestroy(): void {
    if (this.recentReleasesChangedSubscription) {
      this.recentReleasesChangedSubscription.unsubscribe();
    }
  }

  private recentReleasesChanged(recentReleases: RecentRelease[]) {
    const menuRecentReleases = [];
    for (const release of recentReleases) {
      const url = `release/${release.environmentId}/${release.releaseId}/version/${release.releaseVersionId}`;
      menuRecentReleases.push({label: release.name, icon: 'pi pi-fw pi-play', routerLink: [url], roles: [Role.user]});
    }
    this.model[1] = {label: 'Recent', items: menuRecentReleases};
  }
}
