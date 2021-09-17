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
import {EnvironmentReleaseDetailComponent} from './environment-release/environment-release-detail/environment-release-detail.component';
import {EnvironmentReleasesComponent} from './environment-release/environment-releases.component';
import {EnvironmentDetailComponent} from './environments/environment-detail/environment-detail.component';
import {EnvironmentsComponent} from './environments/environments.component';
import {ReleaseHistoryComponent} from './release-history/release-history.component';
import {ReleaseApplicationEnvironmentConfigComponent} from './release-manage/release-application-environment-config/release-application-environment-config.component';
import {ReleaseManageComponent} from './release-manage/release-manage.component';
import {ReleaseDetailComponent} from './releases/release-detail/release-detail.component';
import {ReleasesComponent} from './releases/releases.component';
import {ArgoSettingsComponent} from './settings/argo-settings/argo-settings.component';
import {DockerSettingsComponent} from './settings/docker-settings/docker-settings.component';
import {GitSettingsComponent} from './settings/git-settings/git-settings.component';
import {SettingsComponent} from './settings/settings.component';
import {SettingsGuard} from './settings/settings.guard';
import {AuthGuard} from './util/auth.guard';

const routes: Routes = [
  {
    path: '', component: AppMainComponent, canActivate: [AuthGuard],
    children: [
      {path: '', component: DashboardComponent, canActivate: [SettingsGuard]},

      {path: 'releases', component: ReleasesComponent, canActivate: [SettingsGuard]},
      {path: 'release/:releaseId', component: ReleaseDetailComponent},

      {path: 'release/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseManageComponent},
      {path: 'release/:environmentId/:releaseId/config/:relVersionId/:appVersionId', component: ReleaseApplicationEnvironmentConfigComponent},

      {path: 'releasehistory/:releaseId', component: ReleaseHistoryComponent},

      {path: 'environmentreleases/:environmentId', component: EnvironmentReleasesComponent},
      {path: 'environmentrelease/edit/:environmentId/:releaseId', component: EnvironmentReleaseDetailComponent},
      {path: 'environmentrelease/edit/:environmentId/:releaseId/version/:releaseVersionId', component: EnvironmentReleaseDetailComponent},

      {path: 'environments', component: EnvironmentsComponent, canActivate: [SettingsGuard]},
      {path: 'environment/:id', component: EnvironmentDetailComponent},

      {path: 'applications', component: ApplicationsComponent, canActivate: [SettingsGuard]},
      {path: 'application/:id', component: ApplicationDetailComponent},
      {path: 'application/:appId/version/:versionId', component: ApplicationVersionDetailComponent},

      {path: 'clusters', component: ClustersComponent, canActivate: [SettingsGuard]},
      {path: 'cluster/:id', component: ClusterDetailComponent},

      {
        path: 'settings', component: SettingsComponent,
        children: [
          {path: '', redirectTo: '/settings/git', pathMatch: 'full'},
          {path: 'git', component: GitSettingsComponent},
          {path: 'argo', component: ArgoSettingsComponent},
          {path: 'docker', component: DockerSettingsComponent}
        ]
      }
    ]
  }
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
