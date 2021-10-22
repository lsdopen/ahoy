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

import {ScrollingModule} from '@angular/cdk/scrolling';
import {HttpClientModule} from '@angular/common/http';
import {ErrorHandler, NgModule} from '@angular/core';
import {FlexLayoutModule} from '@angular/flex-layout';
import {FormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {OAuthModule} from 'angular-oauth2-oidc';
import {AccordionModule} from 'primeng/accordion';
import {ConfirmationService, MessageService} from 'primeng/api';
import {AvatarModule} from 'primeng/avatar';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {ButtonModule} from 'primeng/button';
import {CheckboxModule} from 'primeng/checkbox';
import {ChipModule} from 'primeng/chip';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {DialogModule} from 'primeng/dialog';
import {DividerModule} from 'primeng/divider';
import {DropdownModule} from 'primeng/dropdown';
import {DialogService, DynamicDialogModule} from 'primeng/dynamicdialog';
import {InputNumberModule} from 'primeng/inputnumber';
import {InputSwitchModule} from 'primeng/inputswitch';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {MegaMenuModule} from 'primeng/megamenu';
import {MenuModule} from 'primeng/menu';
import {MessagesModule} from 'primeng/messages';
import {ProgressBarModule} from 'primeng/progressbar';
import {RadioButtonModule} from 'primeng/radiobutton';
import {RippleModule} from 'primeng/ripple';
import {SidebarModule} from 'primeng/sidebar';
import {TableModule} from 'primeng/table';
import {TabViewModule} from 'primeng/tabview';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';
import {ToolbarModule} from 'primeng/toolbar';
import {TooltipModule} from 'primeng/tooltip';
import {AppRoutingModule} from './app-routing.module';
import {AppBreadcrumbComponent} from './app.breadcrumb.component';
import {AppBreadcrumbService} from './app.breadcrumb.service';

import {AppComponent} from './app.component';
import {AppConfigComponent} from './app.config.component';
import {AppFooterComponent} from './app.footer.component';
import {AppMainComponent} from './app.main.component';
import {AppMenuComponent} from './app.menu.component';
import {MenuService} from './app.menu.service';
import {AppMenuitemComponent} from './app.menuitem.component';
import {AppTopBarComponent} from './app.topbar.component';
import {ApplicationConfigFilesComponent} from './applications/application-config-files/application-config-files.component';
import {ApplicationDetailComponent} from './applications/application-detail/application-detail.component';
import {ApplicationEnvVariablesComponent} from './applications/application-env-variables/application-env-variables.component';
import {ApplicationSecretDataComponent} from './applications/application-secret-data/application-secret-data.component';
import {ApplicationSecretsComponent} from './applications/application-secrets/application-secrets.component';
import {ApplicationVersionDetailComponent} from './applications/application-version-detail/application-version-detail.component';
import {ApplicationVersionsComponent} from './applications/application-versions/application-versions.component';
import {ApplicationVolumeDetailComponent} from './applications/application-volume-detail/application-volume-detail.component';
import {ApplicationsComponent} from './applications/applications.component';
import {DockerRegistriesComponent} from './applications/docker-registries/docker-registries.component';
import {ClusterDetailComponent} from './clusters/cluster-detail/cluster-detail.component';
import {ClustersComponent} from './clusters/clusters.component';
import {ConfirmDialogComponent} from './components/confirm-dialog/confirm-dialog.component';
import {VerifyValidatorDirective} from './components/confirm-dialog/verify-validator.directive';
import {DescriptionDialogComponent} from './components/description-dialog/description-dialog.component';
import {MultiTabComponent} from './components/multi-tab/multi-tab.component';
import {TabItemNameUniqueValidatorDirective} from './components/multi-tab/tab-item-name-unique-validator.directive';
import {NameUniqueValidatorDirective} from './components/name-unique-validator.directive';
import {VersionUniqueValidatorDirective} from './components/version-unique-validator.directive';
import {DashboardEnvironmentComponent} from './dashboard/dashboard-environment/dashboard-environment.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {AddReleaseDialogComponent} from './environment-release/add-release-dialog/add-release-dialog.component';
import {EnvironmentReleaseDeploymentStatusComponent} from './environment-release/environment-release-deployment-status/environment-release-deployment-status.component';
import {EnvironmentReleaseStatusComponent} from './environment-release/environment-release-status/environment-release-status.component';
import {EnvironmentReleaseVersionsComponent} from './environment-release/environment-release-versions/environment-release-versions.component';
import {EnvironmentReleasesComponent} from './environment-release/environment-releases.component';
import {EnvironmentDetailComponent} from './environments/environment-detail/environment-detail.component';
import {EnvironmentNameUniqueValidatorDirective} from './environments/environment-name-unique-validator.directive';
import {EnvironmentsComponent} from './environments/environments.component';
import {NotificationsComponent} from './notifications/notifications.component';
import {AppAccessDeniedComponent} from './pages/app-access-denied.component';
import {AppErrorComponent} from './pages/app-error.component';
import {AppNotFoundComponent} from './pages/app-not-found.component';
import {ReleaseHistoryComponent} from './release-history/release-history.component';
import {AddApplicationDialogComponent} from './release-manage/add-application-dialog/add-application-dialog.component';
import {ApplicationAllowedValidatorDirective} from './release-manage/add-application-dialog/application-allowed.directive';
import {CopyEnvironmentConfigDialogComponent} from './release-manage/copy-environment-config-dialog/copy-environment-config-dialog.component';
import {PromoteDialogComponent} from './release-manage/promote-dialog/promote-dialog.component';
import {ReleaseApplicationEnvironmentConfigComponent} from './release-manage/release-application-environment-config/release-application-environment-config.component';
import {ReleaseApplicationVersionStatusComponent} from './release-manage/release-application-version-status/release-application-version-status.component';
import {ReleaseApplicationVersionsComponent} from './release-manage/release-application-versions/release-application-versions.component';
import {ReleaseManageComponent} from './release-manage/release-manage.component';
import {UpgradeDialogComponent} from './release-manage/upgrade-dialog/upgrade-dialog.component';
import {AddToEnvironmentDialogComponent} from './releases/add-to-environment-dialog/add-to-environment-dialog.component';
import {ReleaseDetailComponent} from './releases/release-detail/release-detail.component';
import {ReleaseVersionDetailComponent} from './releases/release-version-detail/release-version-detail.component';
import {ReleaseVersionsComponent} from './releases/release-versions/release-versions.component';
import {ReleasesComponent} from './releases/releases.component';
import {ArgoSettingsComponent} from './settings/argo-settings/argo-settings.component';
import {DockerSettingsComponent} from './settings/docker-settings/docker-settings.component';
import {GitSettingsComponent} from './settings/git-settings/git-settings.component';
import {SettingsComponent} from './settings/settings.component';
import {TaskEventsListenerComponent} from './taskevents/task-events-listener/task-events-listener.component';
import {ErrorService} from './util/error.service';
import {MoveDialogComponent} from './environments/move-dialog/move-dialog.component';

@NgModule({
  declarations: [
    AppComponent,
    AppMainComponent,
    AppTopBarComponent,
    AppMenuComponent,
    AppMenuitemComponent,
    AppBreadcrumbComponent,
    AppFooterComponent,
    AppConfigComponent,
    DashboardComponent,
    ApplicationsComponent,
    ApplicationDetailComponent,
    ReleasesComponent,
    ReleaseManageComponent,
    EnvironmentsComponent,
    EnvironmentDetailComponent,
    ReleaseApplicationVersionsComponent,
    AddApplicationDialogComponent,
    PromoteDialogComponent,
    UpgradeDialogComponent,
    MoveDialogComponent,
    ConfirmDialogComponent,
    ClustersComponent,
    ClusterDetailComponent,
    DashboardEnvironmentComponent,
    AddReleaseDialogComponent,
    EnvironmentNameUniqueValidatorDirective,
    ApplicationVersionsComponent,
    ApplicationVersionDetailComponent,
    ApplicationAllowedValidatorDirective,
    NotificationsComponent,
    TaskEventsListenerComponent,
    ReleaseApplicationEnvironmentConfigComponent,
    ApplicationEnvVariablesComponent,
    CopyEnvironmentConfigDialogComponent,
    ReleaseHistoryComponent,
    DescriptionDialogComponent,
    EnvironmentReleaseVersionsComponent,
    EnvironmentReleaseStatusComponent,
    ReleaseApplicationVersionStatusComponent,
    SettingsComponent,
    DockerRegistriesComponent,
    VerifyValidatorDirective,
    GitSettingsComponent,
    ArgoSettingsComponent,
    DockerSettingsComponent,
    ApplicationSecretDataComponent,
    ApplicationVolumeDetailComponent,
    EnvironmentReleaseDeploymentStatusComponent,
    ApplicationConfigFilesComponent,
    ApplicationSecretsComponent,
    EnvironmentReleasesComponent,
    ReleaseDetailComponent,
    AddToEnvironmentDialogComponent,
    MultiTabComponent,
    TabItemNameUniqueValidatorDirective,
    NameUniqueValidatorDirective,
    VersionUniqueValidatorDirective,
    ReleaseVersionsComponent,
    ReleaseVersionDetailComponent,
    AppNotFoundComponent,
    AppAccessDeniedComponent,
    AppErrorComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    HttpClientModule,
    OAuthModule.forRoot({
      resourceServer: {
        allowedUrls: ['http://localhost:8080/data', 'http://localhost:8080/api', '/data', '/api'],
        sendAccessToken: true
      }
    }),
    BrowserAnimationsModule,
    FlexLayoutModule,
    ScrollingModule,
    MegaMenuModule,
    SidebarModule,
    RadioButtonModule,
    InputSwitchModule,
    BreadcrumbModule,
    TooltipModule,
    ButtonModule,
    ToolbarModule,
    ToastModule,
    TableModule,
    InputTextModule,
    RippleModule,
    DropdownModule,
    DialogModule,
    DynamicDialogModule,
    ConfirmPopupModule,
    ProgressBarModule,
    InputTextareaModule,
    ChipModule,
    AvatarModule,
    MenuModule,
    TabViewModule,
    CheckboxModule,
    DividerModule,
    MessagesModule,
    TagModule,
    AccordionModule,
    InputNumberModule
  ],
  providers: [
    {provide: ErrorHandler, useClass: ErrorService},
    MenuService, AppBreadcrumbService, MessageService, ConfirmationService, DialogService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
