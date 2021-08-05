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
import {AppMainComponent} from './app.main.component';

@Component({
    selector: 'app-config',
    template: `
        <p-sidebar #sidebar [(visible)]="configActive" [position]="app.isRTL ? 'left' : 'right'" [blockScroll]="true" [showCloseIcon]="false" [baseZIndex]="1000" styleClass="layout-config p-sidebar-sm fs-small p-p-0">
            <div class="layout-config-panel p-d-flex p-flex-column">
                <div class="p-px-3 p-pt-3">
                    <h5>Theme Customization</h5>
                    <span>Ultima offers different themes for layout, topbar, menu etc.</span>
                </div>

                <hr class="p-mb-0" />

                <div class="layout-config-options p-p-3">
                    <h6>Layout/Theme Scale</h6>
                    <div class="p-d-flex p-ai-center">
                        <button pButton pRipple type="button" icon="pi pi-minus" (click)="decrementScale()" class="p-button-rounded p-button-text" [disabled]="scale === scales[0]"></button>
                        <i class="pi pi-circle-on p-m-1 scale-icon" *ngFor="let s of scales" [ngClass]="{'scale-active': s === scale}"></i>
                        <button pButton pRipple type="button" icon="pi pi-plus" (click)="incrementScale()" class="p-button-rounded p-button-text" [disabled]="scale === scales[scales.length - 1]"></button>
                    </div>

                    <h6>Layout Mode</h6>
                    <div class="p-d-flex">
                        <div class="p-d-flex p-ai-center">
                            <p-radioButton name="layoutMode" value="light" [(ngModel)]="app.layoutMode" inputId="layoutMode1" (onClick)="onLayoutModeChange($event, 'light')"></p-radioButton>
                            <label for="layoutMode1" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Light</label>
                        </div>
                        <div class="p-d-flex p-ai-center" [ngClass]="{'p-ml-4': !app.isRTL, 'p-mr-4': app.isRTL}">
                            <p-radioButton name="layoutMode" value="dark" [(ngModel)]="app.layoutMode" inputId="layoutMode2" (onClick)="onLayoutModeChange($event, 'dark')"></p-radioButton>
                            <label for="layoutMode2" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Dark</label>
                        </div>
                    </div>

                    <h6>Menu Mode</h6>
                    <div class="p-d-flex">
                        <div class="p-d-flex p-flex-column">
                            <div class="p-d-flex p-ai-center">
                                <p-radioButton name="menuMode" value="static" [(ngModel)]="app.menuMode" inputId="menuMode1"></p-radioButton>
                                <label for="menuMode1" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Static</label>
                            </div>
                            <div class="p-d-flex p-ai-center p-mt-3">
                                <p-radioButton name="menuMode" value="horizontal" [(ngModel)]="app.menuMode" inputId="menuMode2"></p-radioButton>
                                <label for="menuMode2" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Horizontal</label>
                            </div>
                        </div>
                        <div class="p-d-flex p-flex-column" [ngClass]="{'p-ml-4': !app.isRTL, 'p-mr-4': app.isRTL}">
                            <div class="p-d-flex p-ai-center">
                                <p-radioButton name="menuMode" value="overlay" [(ngModel)]="app.menuMode" inputId="menuMode4"></p-radioButton>
                                <label for="menuMode4" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Overlay</label>
                            </div>
                            <div class="p-d-flex p-ai-center p-mt-3">
                                <p-radioButton name="menuMode" value="slim" [(ngModel)]="app.menuMode" inputId="menuMode3"></p-radioButton>
                                <label for="menuMode3" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Slim</label>
                            </div>
                        </div>
                    </div>

                    <h6>Inline Menu Position</h6>
                    <div class="p-d-flex">
                        <div class="p-d-flex p-ai-center">
                            <p-radioButton name="inlineMenuPosition" value="top" [(ngModel)]="app.inlineMenuPosition" inputId="inlineMenuPosition1"></p-radioButton>
                            <label for="inlineMenuPosition1" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Top</label>
                        </div>
                        <div class="p-d-flex p-ai-center" [ngClass]="{'p-ml-4': !app.isRTL, 'p-mr-4': app.isRTL}">
                            <p-radioButton name="inlineMenuPosition" value="bottom" [(ngModel)]="app.inlineMenuPosition" inputId="inlineMenuPosition2"></p-radioButton>
                            <label for="inlineMenuPosition2" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Bottom</label>
                        </div>
                        <div class="p-d-flex p-ai-center" [ngClass]="{'p-ml-4': !app.isRTL, 'p-mr-4': app.isRTL}">
                            <p-radioButton name="inlineMenuPosition" value="both" [(ngModel)]="app.inlineMenuPosition" inputId="inlineMenuPosition3"></p-radioButton>
                            <label for="inlineMenuPosition3" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Both</label>
                        </div>
                    </div>

                    <h6>Input Background</h6>
                    <div class="p-d-flex">
                        <div class="p-d-flex p-ai-center">
                            <p-radioButton name="inputStyle" value="outlined" [(ngModel)]="app.inputStyle" inputId="inputStyle1" (onClick)="onInputStyleClick()"></p-radioButton>
                            <label for="inputStyle1" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Outlined</label>
                        </div>
                        <div class="p-d-flex p-ai-center" [ngClass]="{'p-ml-4': !app.isRTL, 'p-mr-4': app.isRTL}">
                            <p-radioButton name="inputStyle" value="filled" [(ngModel)]="app.inputStyle" inputId="inputStyle2" (onClick)="onInputStyleClick()"></p-radioButton>
                            <label for="inputStyle2" [ngClass]="{'p-ml-2': !app.isRTL, 'p-mr-2': app.isRTL}">Filled</label>
                        </div>
                    </div>

                    <h6>Ripple Effect</h6>
                    <p-inputSwitch [ngModel]="app.ripple" (onChange)="appMain.onRippleChange($event)"></p-inputSwitch>

                    <h6>RTL</h6>
                    <p-inputSwitch [ngModel]="app.isRTL" (onChange)="appMain.onRTLChange($event)" styleClass="p-d-block"></p-inputSwitch>

                    <h6>Menu Themes</h6>
                    <div *ngIf="app.layoutMode!=='dark'" class="p-grid">
                        <div *ngFor="let t of menuThemes" class="p-col p-col-fixed">
                            <a style="cursor: pointer" (click)="changeMenuTheme(t)" class="layout-config-color-option" [title]="t.name">
                                <span class="color" [ngStyle]="{'background-color': t.color}"></span>
                                <span class="check p-d-flex p-ai-center p-jc-center" *ngIf="app.menuTheme === t.name">
                                    <i class="pi pi-check" style="color: var(--menu-text-color)"></i>
                                </span>
                            </a>
                        </div>
                    </div>
                    <p *ngIf="app.layoutMode==='dark'">Menu themes are only available in light mode by design as large surfaces can emit too much brightness in dark mode.</p>

                    <h6>Topbar Themes</h6>
                    <div class="p-grid">
                        <div *ngFor="let t of topbarThemes" class="p-col p-col-fixed">
                            <a style="cursor: pointer" (click)="changeTopbarTheme(t)" class="layout-config-color-option" [title]="t.name">
                                <span class="color" [ngStyle]="{'background-color': t.color}"></span>
                                <span class="check p-d-flex p-ai-center p-jc-center" *ngIf="app.topbarTheme === t.name">
                                    <i class="pi pi-check" style="color: var(--topbar-text-color)"></i>
                                </span>
                            </a>
                        </div>
                    </div>

                    <h6>Component Themes</h6>
                    <div class="p-grid">
                        <div *ngFor="let t of themes" class="p-col p-col-fixed">
                            <a style="cursor: pointer" (click)="changeTheme(t.name)" class="layout-config-color-option" [title]="t.name">
                                <span class="color" [ngStyle]="{'background-color': t.color}"></span>
                                <span class="check p-d-flex p-ai-center p-jc-center" *ngIf="theme === t.name">
                                    <i class="pi pi-check" style="color: var(--primary-color-text)"></i>
                                </span>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </p-sidebar>

        <p-button type="button" (click)="configActive = true" icon="pi pi-cog" *ngIf="!configActive" styleClass="layout-config-button"></p-button>
    `
})
export class AppConfigComponent implements OnInit {

    scale: number = 14;

    scales: number[] = [12,13,14,15,16];

    themes: any[];

    menuThemes: any[];

    menuTheme = 'light';

    topbarThemes: any[];

    topbarTheme: string = 'bluegrey';

    theme: string = 'orange';

    matchingMenuTheme: boolean = false;

    matchingTopbarTheme: boolean = false;

    selectedMenuTheme: any;

    selectedTopbarTheme: any;

    configActive: boolean = false;

    isInputBackgroundChanged: boolean = false;

    constructor(public appMain: AppMainComponent, public app: AppComponent) {}

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

        this.selectedMenuTheme = this.menuThemes.find(theme => theme.name === this.menuTheme);
        this.selectedTopbarTheme = this.topbarThemes.find(theme => theme.name === this.topbarTheme);
    }

    decrementScale() {
        this.scale--;
        this.applyScale();
    }

    incrementScale() {
        this.scale++;
        this.applyScale();
    }

    applyScale() {
        document.documentElement.style.fontSize = this.scale + 'px';
    }

    onInputStyleClick() {
        this.isInputBackgroundChanged = true;
    }

    onLayoutModeChange(event, mode) {
        this.app.layoutMode = mode;

        if (!this.isInputBackgroundChanged) {
            this.app.inputStyle = mode === 'dark' ? 'filled' : 'outlined';
        }

        if (mode === 'dark') {
            this.app.menuTheme = 'dark';
            this.app.topbarTheme = 'dark';
        }
        else {
            this.app.menuTheme = 'light';
            this.app.topbarTheme = 'blue';
        }

        const layoutLink: HTMLLinkElement = document.getElementById('layout-css') as HTMLLinkElement;
        const layoutHref = 'assets/layout/css/layout-' + this.app.layoutMode + '.css';
        this.replaceLink(layoutLink, layoutHref);

        const themeLink = document.getElementById('theme-css');
        const urlTokens = themeLink.getAttribute('href').split('/');
        urlTokens[urlTokens.length - 1] = 'theme-' + this.app.layoutMode + '.css';
        const newURL = urlTokens.join('/');

        this.replaceLink(themeLink, newURL, this.appMain['refreshChart']);
    }

    changeTheme(theme) {
        this.theme = theme;

        const themeLink: HTMLLinkElement = document.getElementById('theme-css') as HTMLLinkElement;
        const themeHref = 'assets/theme/' + theme + '/theme-' + this.app.layoutMode + '.css';
        this.replaceLink(themeLink, themeHref);
    }

    changeMenuTheme(theme) {
        this.selectedMenuTheme = theme;
        this.app.menuTheme = theme.name;
    }

    changeTopbarTheme(theme) {
        this.selectedTopbarTheme = theme;
        this.app.topbarTheme = theme.name;

        const appLogoLink: HTMLImageElement = document.getElementById('app-logo') as HTMLImageElement;

        if (theme.name == 'white' || theme.name == 'yellow' || theme.name == 'amber'  || theme.name == 'orange' || theme.name == 'lime') {
            appLogoLink.src = 'assets/layout/images/logo-dark.svg';
        }
        else {
            appLogoLink.src = 'assets/layout/images/logo-light.svg';
        }
    }

    isIE() {
        return /(MSIE|Trident\/|Edge\/)/i.test(window.navigator.userAgent);
    }

    replaceLink(linkElement, href, callback?) {
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
