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

import {ErrorHandler, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {OAuthModule} from 'angular-oauth2-oidc';
import {ConfirmationService, MessageService} from 'primeng/api';
import {AvatarModule} from 'primeng/avatar';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {ButtonModule} from 'primeng/button';
import {DividerModule} from 'primeng/divider';
import {DialogService} from 'primeng/dynamicdialog';
import {ProgressBarModule} from 'primeng/progressbar';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {AhoyComponentsModule, AppBreadcrumbService} from 'projects/ahoy-components/src/public-api';
import {AppRoutingModule} from './app-routing.module';
import {AppBreadcrumbComponent} from './app.breadcrumb.component';
import {AppComponent} from './app.component';
import {AppFooterComponent} from './app.footer.component';
import {AppMainComponent} from './app.main.component';
import {AppMenuComponent} from './app.menu.component';
import {MenuService} from './app.menu.service';
import {AppMenuitemComponent} from './app.menuitem.component';
import {AppTopBarComponent} from './app.topbar.component';
import {NotificationsComponent} from './notifications/notifications.component';
import {ErrorService} from './util/error.service';
import {environment} from '../environments/environment';

@NgModule({
  declarations: [
    AppComponent,
    AppMainComponent,
    AppTopBarComponent,
    AppMenuComponent,
    AppMenuitemComponent,
    AppBreadcrumbComponent,
    NotificationsComponent,
    AppFooterComponent
  ],
  imports: [
    OAuthModule.forRoot({
      resourceServer: {
        allowedUrls: ['http://localhost:8080/data', 'http://localhost:8080/api', '/data', '/api'],
        sendAccessToken: true
      }
    }),
    AhoyComponentsModule,
    AppRoutingModule,
    AvatarModule,
    BreadcrumbModule,
    BrowserModule,
    ButtonModule,
    DividerModule,
    ProgressBarModule,
    RippleModule,
    ToastModule,
  ],
  providers: [
    {provide: ErrorHandler, useClass: ErrorService},
    {provide: 'environment', useValue: environment},
    MenuService, AppBreadcrumbService, MessageService, ConfirmationService, DialogService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
