/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
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
import {DashboardComponent} from './dashboard/dashboard.component';
import {ApplicationsComponent} from './applications/applications.component';
import {ApplicationDetailComponent} from './applications/application-detail/application-detail.component';
import {ReleasesComponent} from './releases/releases.component';
import {ReleaseDetailComponent} from './releases/release-detail/release-detail.component';
import {ReleaseManageComponent} from './releases/release-manage/release-manage.component';
import {EnvironmentsComponent} from './environments/environments.component';
import {EnvironmentDetailComponent} from './environments/environment-detail/environment-detail.component';
import {ClustersComponent} from './clusters/clusters.component';
import {ClusterDetailComponent} from './clusters/cluster-detail/cluster-detail.component';
import {ApplicationVersionDetailComponent} from './applications/application-version-detail/application-version-detail.component';
import {ReleaseApplicationEnvironmentConfigComponent} from './releases/release-application-environment-config/release-application-environment-config.component';
import {ReleaseHistoryComponent} from './release-history/release-history.component';
import {SettingsComponent} from './settings/settings.component';
import {SettingsGuard} from './settings/settings.guard';
import {GitSettingsComponent} from './settings/git-settings/git-settings.component';
import {ArgoSettingsComponent} from './settings/argo-settings/argo-settings.component';
import {DockerSettingsComponent} from './settings/docker-settings/docker-settings.component';
import {AuthGuard} from "./util/auth.guard";

const routes: Routes = [
  {path: '', redirectTo: '/dashboard', pathMatch: 'full', canActivate: [AuthGuard]},
  {path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard, SettingsGuard]},
  {path: 'releases', component: ReleasesComponent, canActivate: [AuthGuard, SettingsGuard]},
  {path: 'release/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseManageComponent, canActivate: [AuthGuard]},
  {path: 'release/edit/:environmentId/:releaseId', component: ReleaseDetailComponent, canActivate: [AuthGuard]},
  {path: 'release/edit/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseDetailComponent, canActivate: [AuthGuard]},
  {path: 'release/:environmentId/:releaseId/config/:relVersionId/:appVersionId', component: ReleaseApplicationEnvironmentConfigComponent, canActivate: [AuthGuard]},
  {path: 'releasehistory/:releaseId', component: ReleaseHistoryComponent, canActivate: [AuthGuard]},
  {path: 'environments', component: EnvironmentsComponent, canActivate: [AuthGuard, SettingsGuard]},
  {path: 'environment/:id', component: EnvironmentDetailComponent, canActivate: [AuthGuard]},
  {path: 'applications', component: ApplicationsComponent, canActivate: [AuthGuard, SettingsGuard]},
  {path: 'application/:id', component: ApplicationDetailComponent, canActivate: [AuthGuard]},
  {path: 'application/:appId/version/:versionId', component: ApplicationVersionDetailComponent, canActivate: [AuthGuard]},
  {path: 'clusters', component: ClustersComponent, canActivate: [AuthGuard, SettingsGuard]},
  {path: 'cluster/:id', component: ClusterDetailComponent, canActivate: [AuthGuard]},
  {
    path: 'settings', component: SettingsComponent, canActivate: [AuthGuard], children: [
      {path: '', redirectTo: '/settings/git', pathMatch: 'full'},
      {path: 'git', component: GitSettingsComponent},
      {path: 'argo', component: ArgoSettingsComponent},
      {path: 'docker', component: DockerSettingsComponent}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    useHash: true,
    initialNavigation: false
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
