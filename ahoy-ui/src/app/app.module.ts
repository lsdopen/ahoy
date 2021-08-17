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

import {BrowserModule} from '@angular/platform-browser';
import {ErrorHandler, NgModule} from '@angular/core';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FlexLayoutModule} from '@angular/flex-layout';

import {AppComponent} from './app.component';
import {AppMainComponent} from './app.main.component';
import {AppTopBarComponent} from './app.topbar.component';
import {AppMenuComponent} from './app.menu.component';
import {AppMenuitemComponent} from './app.menuitem.component';
import {AppInlineMenuComponent} from './app.inlinemenu.component';
import {AppBreadcrumbComponent} from './app.breadcrumb.component';
import {AppFooterComponent} from './app.footer.component';
import {AppConfigComponent} from './app.config.component';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatOptionModule} from '@angular/material/core';
import {MatDialogModule} from '@angular/material/dialog';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatGridListModule} from '@angular/material/grid-list';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatListModule} from '@angular/material/list';
import {MatMenuModule} from '@angular/material/menu';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSelectModule} from '@angular/material/select';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTableModule} from '@angular/material/table';
import {MatTabsModule} from '@angular/material/tabs';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {AppRoutingModule} from './app-routing.module';
import {DashboardComponent} from './dashboard/dashboard.component';
import {ApplicationsComponent} from './applications/applications.component';
import {ApplicationDetailComponent} from './applications/application-detail/application-detail.component';
import {FormsModule} from '@angular/forms';
import {HttpClientModule} from '@angular/common/http';
import {ReleasesComponent} from './releases/releases.component';
import {ReleaseDetailComponent} from './releases/release-detail/release-detail.component';
import {DetailComponent} from './components/detail/detail.component';
import {ContentComponent} from './layout/content/content.component';
import {ButtonBarComponent} from './components/button-bar/button-bar.component';
import {ReleaseManageComponent} from './releases/release-manage/release-manage.component';
import {EnvironmentsComponent} from './environments/environments.component';
import {EnvironmentDetailComponent} from './environments/environment-detail/environment-detail.component';
import {ReleaseApplicationVersionsComponent} from './releases/release-application-versions/release-application-versions.component';
import {AddApplicationDialogComponent} from './releases/add-application-dialog/add-application-dialog.component';
import {PromoteDialogComponent} from './releases/promote-dialog/promote-dialog.component';
import {UpgradeDialogComponent} from './releases/upgrade-dialog/upgrade-dialog.component';
import {ConfirmDialogComponent} from './components/confirm-dialog/confirm-dialog.component';
import {ClustersComponent} from './clusters/clusters.component';
import {ClusterDetailComponent} from './clusters/cluster-detail/cluster-detail.component';
import {DashboardEnvironmentComponent} from './dashboard/dashboard-environment/dashboard-environment.component';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {AddReleaseDialogComponent} from './releases/add-release-dialog/add-release-dialog.component';
import {ReleaseNameUniqueValidatorDirective} from './releases/release-name-unique-validator.directive';
import {ReleaseVersionUniqueValidatorDirective} from './releases/release-version-unique-validator.directive';
import {ApplicationVersionsComponent} from './applications/application-versions/application-versions.component';
import {ApplicationVersionDetailComponent} from './applications/application-version-detail/application-version-detail.component';
import {ApplicationNameUniqueValidatorDirective} from './applications/application-name-unique-validator.directive';
import {ApplicationVersionUniqueValidatorDirective} from './applications/application-version-unique-validator.directive';
import {NotificationsComponent} from './notifications/notifications.component';
import {TaskEventsListenerComponent} from './taskevents/task-events-listener/task-events-listener.component';
import {ReleaseApplicationEnvironmentConfigComponent} from './releases/release-application-environment-config/release-application-environment-config.component';
import {ApplicationEnvVariablesComponent} from './applications/application-env-variables/application-env-variables.component';
import {CopyEnvironmentConfigDialogComponent} from './releases/copy-environment-config-dialog/copy-environment-config-dialog.component';
import {ErrorService} from './util/error.service';
import {ReleaseHistoryComponent} from './release-history/release-history.component';
import {DescriptionDialogComponent} from './components/description-dialog/description-dialog.component';
import {EnvironmentReleaseVersionsComponent} from './environment-release/environment-release-versions/environment-release-versions.component';
import {EnvironmentReleaseStatusComponent} from './environment-release/environment-release-status/environment-release-status.component';
import {ReleaseApplicationVersionStatusComponent} from './releases/release-application-version-status/release-application-version-status.component';
import {EnvironmentNameUniqueValidatorDirective} from './environments/environment-name-unique-validator.directive';
import {SettingsComponent} from './settings/settings.component';
import {MatRadioModule} from '@angular/material/radio';
import {DockerRegistriesComponent} from './applications/docker-registries/docker-registries.component';
import {ApplicationAllowedValidatorDirective} from './releases/add-application-dialog/application-allowed.directive';
import {VerifyValidatorDirective} from './components/confirm-dialog/verify-validator.directive';
import {GitSettingsComponent} from './settings/git-settings/git-settings.component';
import {ArgoSettingsComponent} from './settings/argo-settings/argo-settings.component';
import {DockerSettingsComponent} from './settings/docker-settings/docker-settings.component';
import {OAuthModule} from 'angular-oauth2-oidc';
import {ApplicationSecretDataComponent} from './applications/application-secret-data/application-secret-data.component';
import {ApplicationVolumeDetailComponent} from './applications/application-volume-detail/application-volume-detail.component';
import {MegaMenuModule} from 'primeng/megamenu';
import {SidebarModule} from 'primeng/sidebar';
import {RadioButtonModule} from 'primeng/radiobutton';
import {InputSwitchModule} from 'primeng/inputswitch';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {TooltipModule} from 'primeng/tooltip';
import {MenuService} from './app.menu.service';
import {AppBreadcrumbService} from './app.breadcrumb.service';
import {ButtonModule} from 'primeng/button';
import {ToolbarModule} from 'primeng/toolbar';
import {ToastModule} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {InputTextModule} from 'primeng/inputtext';
import {RippleModule} from 'primeng/ripple';
import {DropdownModule} from 'primeng/dropdown';
import {DialogModule} from 'primeng/dialog';
import {DialogService, DynamicDialogModule} from 'primeng/dynamicdialog';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {ConfirmationService, MessageService} from 'primeng/api';
import {ProgressBarModule} from 'primeng/progressbar';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {ChipModule} from 'primeng/chip';
import {AvatarModule} from 'primeng/avatar';
import {MenuModule} from 'primeng/menu';
import {TabViewModule} from 'primeng/tabview';
import {CheckboxModule} from 'primeng/checkbox';
import {DividerModule} from 'primeng/divider';
import {MessagesModule} from 'primeng/messages';
import {TagModule} from 'primeng/tag';
import {EnvironmentReleaseDeploymentStatusComponent} from './environment-release/environment-release-deployment-status/environment-release-deployment-status.component';

@NgModule({
  declarations: [
    AppComponent,
    AppMainComponent,
    AppTopBarComponent,
    AppMenuComponent,
    AppMenuitemComponent,
    AppInlineMenuComponent,
    AppBreadcrumbComponent,
    AppFooterComponent,
    AppConfigComponent,
    DashboardComponent,
    ApplicationsComponent,
    ApplicationDetailComponent,
    ReleasesComponent,
    ReleaseDetailComponent,
    DetailComponent,
    ContentComponent,
    ButtonBarComponent,
    ReleaseManageComponent,
    EnvironmentsComponent,
    EnvironmentDetailComponent,
    ReleaseApplicationVersionsComponent,
    AddApplicationDialogComponent,
    PromoteDialogComponent,
    UpgradeDialogComponent,
    ConfirmDialogComponent,
    ClustersComponent,
    ClusterDetailComponent,
    DashboardEnvironmentComponent,
    AddReleaseDialogComponent,
    ReleaseNameUniqueValidatorDirective,
    ReleaseVersionUniqueValidatorDirective,
    EnvironmentNameUniqueValidatorDirective,
    ApplicationVersionsComponent,
    ApplicationVersionDetailComponent,
    ApplicationNameUniqueValidatorDirective,
    ApplicationVersionUniqueValidatorDirective,
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
    EnvironmentReleaseDeploymentStatusComponent
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
    MatIconModule,
    MatToolbarModule,
    MatSidenavModule,
    MatButtonModule,
    MatListModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatCardModule,
    MatDialogModule,
    MatOptionModule,
    MatSelectModule,
    MatGridListModule,
    MatTableModule,
    ScrollingModule,
    MatSnackBarModule,
    MatTabsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatExpansionModule,
    MatCheckboxModule,
    MatRadioModule,
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
    TagModule
  ],
  providers: [
    {provide: ErrorHandler, useClass: ErrorService},
    MenuService, AppBreadcrumbService, MessageService, ConfirmationService, DialogService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
