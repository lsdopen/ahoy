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
import {ActivatedRoute} from '@angular/router';
import {MenuItem, Message} from 'primeng/api';
import {Role} from '../util/auth';
import {AuthService} from '../util/auth.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  items: MenuItem[];
  msgs: Message[] = [];

  constructor(private route: ActivatedRoute,
              private authService: AuthService) {
  }

  ngOnInit(): void {
    this.items = [
      {
        label: 'Settings',
        items: [
          {label: 'Git', icon: 'pi pi-fw pi-github', routerLink: ['/settings/git'], disabled: !this.authService.hasOneOfRole([Role.admin])},
          {label: 'Argo', icon: 'pi pi-fw pi-sitemap', routerLink: ['/settings/argo'], disabled: !this.authService.hasOneOfRole([Role.admin])},
          {label: 'Docker', icon: 'pi pi-fw pi-th-large', routerLink: ['/settings/docker'], disabled: !this.authService.hasOneOfRole([Role.admin, Role.releasemanager, Role.developer])},
          {label: 'Appearance', icon: 'pi pi-fw pi-image', routerLink: ['/settings/appearance'], disabled: !this.authService.hasOneOfRole([Role.user])}
        ]
      }
    ];

    this.route.children.map(r => {
      const setup = Boolean(r.snapshot.queryParamMap.get('setup'));
      if (setup) {
        this.msgs = [];
        this.msgs.push({ severity: 'warn', summary: 'Please note', detail: 'In order to continue you need to set up the following:' });
      }
    });
  }
}
