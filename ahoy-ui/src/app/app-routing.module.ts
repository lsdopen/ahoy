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

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {AppMainComponent} from './app.main.component';
import {ApplicationDetailComponent} from './applications/application-detail/application-detail.component';
import {ApplicationVersionDetailComponent} from './applications/application-version-detail/application-version-detail.component';
import {ApplicationsComponent} from './applications/applications.component';
import {ClusterDetailComponent} from './clusters/cluster-detail/cluster-detail.component';
import {ClustersComponent} from './clusters/clusters.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {EnvironmentReleasesComponent} from './environment-release/environment-releases.component';
import {EnvironmentDetailComponent} from './environments/environment-detail/environment-detail.component';
import {EnvironmentsComponent} from './environments/environments.component';
import {AppAccessDeniedComponent} from './pages/app-access-denied.component';
import {AppErrorComponent} from './pages/app-error.component';
import {AppNotFoundComponent} from './pages/app-not-found.component';
import {ReleaseHistoryComponent} from './release-history/release-history.component';
import {ReleaseApplicationEnvironmentConfigComponent} from './release-manage/release-application-environment-config/release-application-environment-config.component';
import {ReleaseManageComponent} from './release-manage/release-manage.component';
import {ReleaseDetailComponent} from './releases/release-detail/release-detail.component';
import {ReleaseVersionDetailComponent} from './releases/release-version-detail/release-version-detail.component';
import {ReleasesComponent} from './releases/releases.component';
import {ArgoSettingsComponent} from './settings/argo-settings/argo-settings.component';
import {DockerSettingsComponent} from './settings/docker-settings/docker-settings.component';
import {GitSettingsComponent} from './settings/git-settings/git-settings.component';
import {SettingsComponent} from './settings/settings.component';
import {SettingsGuard} from './settings/settings.guard';
import {Role} from './util/auth';
import {AuthGuard} from './util/auth.guard';

const routes: Routes = [
  {
    path: '', component: AppMainComponent, canActivate: [AuthGuard],
    children: [
      {
        path: '', component: DashboardComponent,
        canActivate: [AuthGuard, SettingsGuard], data: {roles: [Role.user]}
      },

      // Releases
      {
        path: 'releases', component: ReleasesComponent,
        canActivate: [AuthGuard, SettingsGuard], data: {roles: [Role.user]}
      },
      {
        path: 'release/:releaseId', component: ReleaseDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },
      {
        path: 'release/:releaseId/version/:releaseVersionId', component: ReleaseVersionDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },

      // Release Manage
      {
        path: 'release/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseManageComponent,
        canActivate: [AuthGuard], data: {roles: [Role.user]}
      },
      {
        path: 'release/:environmentId/:releaseId/config/:relVersionId/:appVersionId', component: ReleaseApplicationEnvironmentConfigComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },

      // Release History
      {
        path: 'releasehistory/:releaseId', component: ReleaseHistoryComponent,
        canActivate: [AuthGuard], data: {roles: [Role.user]}
      },

      // Environment Releases
      {
        path: 'environmentreleases/:environmentId', component: EnvironmentReleasesComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager]}
      },

      // Environments
      {
        path: 'environments', component: EnvironmentsComponent,
        canActivate: [AuthGuard, SettingsGuard], data: {roles: [Role.admin, Role.releasemanager]}
      },
      {
        path: 'environment/:id', component: EnvironmentDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager]}
      },

      // Applications
      {
        path: 'applications', component: ApplicationsComponent,
        canActivate: [AuthGuard, SettingsGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },
      {
        path: 'application/:id', component: ApplicationDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },
      {
        path: 'application/:appId/version/:versionId', component: ApplicationVersionDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}
      },

      // Clusters
      {
        path: 'clusters', component: ClustersComponent,
        canActivate: [AuthGuard, SettingsGuard], data: {roles: [Role.admin]}
      },
      {
        path: 'cluster/:id', component: ClusterDetailComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin]}
      },

      // Settings
      {
        path: 'settings', component: SettingsComponent,
        canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]},
        children: [
          {path: 'git', component: GitSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin]}},
          {path: 'argo', component: ArgoSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin]}},
          {path: 'docker', component: DockerSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}}
        ]
      },

      // Errors
      {path: 'access', component: AppAccessDeniedComponent},
      {path: 'notfound', component: AppNotFoundComponent},
    ]
  },
  {path: 'error', component: AppErrorComponent},
  {path: '**', redirectTo: '/notfound'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    useHash: true,
    initialNavigation: 'disabled',
    relativeLinkResolution: 'legacy',
    scrollPositionRestoration: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
