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

import {Component, OnInit} from '@angular/core';
import {AppComponent} from './app.component';

@Component({
    selector: 'app-menu',
    template: `
        <ul class="layout-menu">
            <li app-menuitem *ngFor="let item of model; let i = index;" [item]="item" [index]="i" [root]="true"></li>
        </ul>
    `
})
export class AppMenuComponent implements OnInit {

    model: any[];

    constructor(public app: AppComponent) {}

    ngOnInit() {
        this.model = [
            {
                label: 'Favorites', icon: 'pi pi-fw pi-home',
                items: [
                    {label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/']},
                ]
            },
            {
                label: 'Manage', icon: 'pi pi-fw pi-star', routerLink: ['/manage'],
                items: [
                    {label: 'Releases', icon: 'pi pi-fw pi-forward', routerLink: ['/releases']},
                    {label: 'Environments', icon: 'pi pi-fw pi-folder', routerLink: ['/environments']},
                    {label: 'Applications', icon: 'pi pi-fw pi-image', routerLink: ['/applications']},
                    {label: 'Clusters', icon: 'pi pi-fw pi-table', routerLink: ['/clusters']},
                ]
            }
        ];
    }
}
