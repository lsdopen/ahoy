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

const routes: Routes = [
  {path: '', redirectTo: '/dashboard', pathMatch: 'full'},
  {path: 'dashboard', component: DashboardComponent, canActivate: [SettingsGuard]},
  {path: 'releases', component: ReleasesComponent, canActivate: [SettingsGuard]},
  {path: 'release/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseManageComponent},
  {path: 'release/edit/:environmentId/:releaseId', component: ReleaseDetailComponent},
  {path: 'release/edit/:environmentId/:releaseId/version/:releaseVersionId', component: ReleaseDetailComponent},
  {path: 'release/:environmentId/:releaseId/config/:relVersionId/:appVersionId', component: ReleaseApplicationEnvironmentConfigComponent},
  {path: 'releasehistory/:releaseId', component: ReleaseHistoryComponent},
  {path: 'environments', component: EnvironmentsComponent, canActivate: [SettingsGuard]},
  {path: 'environment/:id', component: EnvironmentDetailComponent},
  {path: 'applications', component: ApplicationsComponent, canActivate: [SettingsGuard]},
  {path: 'application/:id', component: ApplicationDetailComponent},
  {path: 'application/:appId/version/:versionId', component: ApplicationVersionDetailComponent},
  {path: 'clusters', component: ClustersComponent, canActivate: [SettingsGuard]},
  {path: 'cluster/:id', component: ClusterDetailComponent},
  {
    path: 'settings', component: SettingsComponent, children: [
      {path: '', redirectTo: '/settings/git', pathMatch: 'full'},
      {path: 'git', component: GitSettingsComponent},
      {path: 'argo', component: ArgoSettingsComponent},
      {path: 'docker', component: DockerSettingsComponent}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
