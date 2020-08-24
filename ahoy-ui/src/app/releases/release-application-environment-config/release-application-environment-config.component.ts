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

import {Component, OnInit} from '@angular/core';
import {Application, ApplicationEnvironmentConfig, ApplicationEnvironmentConfigId, ApplicationSecret, ApplicationVersion} from '../../applications/application';
import {LoggerService} from '../../util/logger.service';
import {ActivatedRoute} from '@angular/router';
import {ApplicationService} from '../../applications/application.service';
import {Location} from '@angular/common';
import {EnvironmentRelease, EnvironmentReleaseId} from '../../environment-release/environment-release';
import {Environment} from '../../environments/environment';
import {Release, ReleaseVersion} from '../release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {ReleasesService} from '../releases.service';
import {flatMap} from 'rxjs/operators';

@Component({
  selector: 'app-release-application-environment-config',
  templateUrl: './release-application-environment-config.component.html',
  styleUrls: ['./release-application-environment-config.component.scss']
})
export class ReleaseApplicationEnvironmentConfigComponent implements OnInit {
  private exampleRouteHost: string;
  config: ApplicationEnvironmentConfig;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  applicationVersion: ApplicationVersion;
  routeCategory = false;
  environmentVariablesCategory = false;
  configFileCategory = false;
  secretsCategory = false;
  selectedSecretIndex: number;

  constructor(
    private log: LoggerService,
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private environmentReleaseService: EnvironmentReleaseService,
    private releasesService: ReleasesService,
    private location: Location) {
  }

  ngOnInit() {
    const environmentId = +this.route.snapshot.paramMap.get('environmentId');
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');
    const releaseVersionId = +this.route.snapshot.paramMap.get('relVersionId');
    const applicationVersionId = +this.route.snapshot.paramMap.get('appVersionId');

    const id = new ApplicationEnvironmentConfigId();
    id.environmentReleaseId = EnvironmentReleaseId.new(environmentId, releaseId);
    id.releaseVersionId = releaseVersionId;
    id.applicationVersionId = applicationVersionId;

    this.environmentReleaseService.get(environmentId, releaseId)
      .pipe(
        flatMap((environmentRelease) => {
          this.environmentRelease = environmentRelease;
          return this.releasesService.getVersion(releaseVersionId);
        }),
        flatMap((releaseVersion: ReleaseVersion) => {
          this.releaseVersion = releaseVersion;
          return this.applicationService.getVersion(applicationVersionId);
        }),
        flatMap((applicationVersion: ApplicationVersion) => {
          this.applicationVersion = applicationVersion;
          return this.applicationService.getEnvironmentConfig(id);
        }))
      .subscribe((config) => {
        this.config = config;

        const releaseName = (this.environmentRelease.release as Release).name;
        const appName = (this.applicationVersion.application as Application).name;
        const envName = (this.environmentRelease.environment as Environment).name;
        const clusterHost = (this.environmentRelease.environment as Environment).cluster.host;
        this.exampleRouteHost = `${releaseName}-${appName}-${envName}.${clusterHost}`;
        this.setCategoriesExpanded();
      });
  }

  private setCategoriesExpanded() {
    if (this.config.routeHostname) {
      this.routeCategory = true;
    }

    if (this.config.environmentVariables && Object.keys(this.config.environmentVariables).length > 0) {
      this.environmentVariablesCategory = true;
    }

    if (this.config.secrets.length > 0) {
      this.secretsCategory = true;
      this.selectedSecretIndex = 0;
    }

    if (this.config.configFileName) {
      this.configFileCategory = true;
    }
  }

  save() {
    this.applicationService.saveEnvironmentConfig(this.config)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.location.back();
  }

  routeExpand() {
    if (!this.config.routeHostname) {
      this.config.routeHostname = this.exampleRouteHost;
    }
  }

  addSecret() {
    let applicationSecret = new ApplicationSecret();
    applicationSecret.data = {};
    this.config.secrets.push(applicationSecret);
    this.selectedSecretIndex = this.config.secrets.length - 1;
  }

  deleteSecret() {
    this.config.secrets.splice(this.selectedSecretIndex, 1);
    this.selectedSecretIndex = this.selectedSecretIndex - 1;
  }

  secretInUse(): boolean {
    let secret = this.config.secrets[this.selectedSecretIndex];
    if (secret && secret.name) {
      let inUseInEnvironmentVariables = this.config.environmentVariables
        .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
      return inUseInEnvironmentVariables;
    }
    return false;
  }
}
