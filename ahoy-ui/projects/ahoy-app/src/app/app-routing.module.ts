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

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {
  AppAccessDeniedComponent,
  AppearanceSettingsComponent,
  AppErrorComponent,
  ApplicationDetailComponent,
  ApplicationsComponent,
  ApplicationVersionDetailComponent,
  AppNotFoundComponent,
  ArgoSettingsComponent,
  AuthGuard,
  ClusterDetailComponent,
  ClustersComponent,
  DashboardComponent,
  DockerSettingsComponent,
  EnvironmentDetailComponent,
  EnvironmentReleasesComponent,
  EnvironmentsComponent,
  GitSettingsComponent,
  ReleaseApplicationEnvironmentConfigComponent,
  ReleaseDetailComponent,
  ReleaseHistoryComponent,
  ReleaseManageComponent,
  ReleaseResourcesComponent,
  ReleasesComponent,
  ReleaseVersionDetailComponent,
  Role,
  SettingsComponent,
  SettingsGuard
} from 'projects/ahoy-components/src/public-api';
import {AppMainComponent} from './app.main.component';

const routes: Routes = [
  {
    path: '', component: AppMainComponent,
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
      {
        path: 'release/:environmentId/:releaseId/resources', component: ReleaseResourcesComponent,
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
        canActivate: [AuthGuard], data: {roles: [Role.user]},
        children: [
          {path: 'git', component: GitSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin]}},
          {path: 'argo', component: ArgoSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin]}},
          {path: 'docker', component: DockerSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.admin, Role.releasemanager, Role.developer]}},
          {path: 'appearance', component: AppearanceSettingsComponent, canActivate: [AuthGuard], data: {roles: [Role.user]}}
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
    // we use hash navigation as we load the app from a spring boot server and it would try and load pages as resource from the server otherwise
    useHash: true,
    initialNavigation: 'disabled',
    relativeLinkResolution: 'legacy',
    scrollPositionRestoration: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
