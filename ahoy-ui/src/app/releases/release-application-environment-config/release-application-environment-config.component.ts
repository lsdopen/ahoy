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
import {Application, ApplicationConfig, ApplicationEnvironmentConfig, ApplicationEnvironmentConfigId, ApplicationSecret, ApplicationVersion, ApplicationVolume} from '../../applications/application';
import {LoggerService} from '../../util/logger.service';
import {ActivatedRoute} from '@angular/router';
import {ApplicationService} from '../../applications/application.service';
import {Location} from '@angular/common';
import {EnvironmentRelease, EnvironmentReleaseId} from '../../environment-release/environment-release';
import {Environment} from '../../environments/environment';
import {Release, ReleaseVersion} from '../release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {ReleasesService} from '../releases.service';
import {mergeMap} from 'rxjs/operators';

@Component({
  selector: 'app-release-application-environment-config',
  templateUrl: './release-application-environment-config.component.html',
  styleUrls: ['./release-application-environment-config.component.scss']
})
export class ReleaseApplicationEnvironmentConfigComponent implements OnInit {
  private exampleRouteHost: string;
  environmentConfig: ApplicationEnvironmentConfig;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  applicationVersion: ApplicationVersion;
  routeCategory = false;
  environmentVariablesCategory = false;
  configFileCategory = false;
  selectedConfigIndex: number;
  volumesCategory = false;
  selectedVolumeIndex: number;
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
        mergeMap((environmentRelease) => {
          this.environmentRelease = environmentRelease;
          return this.releasesService.getVersion(releaseVersionId);
        }),
        mergeMap((releaseVersion: ReleaseVersion) => {
          this.releaseVersion = releaseVersion;
          return this.applicationService.getVersion(applicationVersionId);
        }),
        mergeMap((applicationVersion: ApplicationVersion) => {
          this.applicationVersion = applicationVersion;
          return this.applicationService.getEnvironmentConfig(id);
        }))
      .subscribe((config) => {
        this.environmentConfig = config;

        const releaseName = (this.environmentRelease.release as Release).name;
        const appName = (this.applicationVersion.application as Application).name;
        const envName = (this.environmentRelease.environment as Environment).name;
        const clusterHost = (this.environmentRelease.environment as Environment).cluster.host;
        this.exampleRouteHost = `${releaseName}-${appName}-${envName}.${clusterHost}`;
        this.setCategoriesExpanded();
      });
  }

  private setCategoriesExpanded() {
    if (this.environmentConfig.routeHostname) {
      this.routeCategory = true;
    }

    if (this.environmentConfig.environmentVariables && Object.keys(this.environmentConfig.environmentVariables).length > 0) {
      this.environmentVariablesCategory = true;
    }

    if (this.environmentConfig.configs.length > 0) {
      this.configFileCategory = true;
      this.selectedConfigIndex = 0;
    }

    if (this.environmentConfig.volumes.length > 0) {
      this.volumesCategory = true;
      this.selectedVolumeIndex = 0;
    }

    if (this.environmentConfig.secrets.length > 0) {
      this.secretsCategory = true;
      this.selectedSecretIndex = 0;
    }
  }

  save() {
    this.applicationService.saveEnvironmentConfig(this.environmentConfig)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.location.back();
  }

  routeExpand() {
    if (!this.environmentConfig.routeHostname) {
      this.environmentConfig.routeHostname = this.exampleRouteHost;
    }
  }

  addConfig() {
    this.environmentConfig.configs.push(new ApplicationConfig());
    this.selectedConfigIndex = this.environmentConfig.configs.length - 1;
  }

  deleteConfig() {
    this.environmentConfig.configs.splice(this.selectedConfigIndex, 1);
    this.selectedConfigIndex = this.selectedConfigIndex - 1;
  }

  addVolume() {
    this.environmentConfig.volumes.push(new ApplicationVolume());
    this.selectedVolumeIndex = this.applicationVersion.volumes.length - 1;
  }

  deleteVolume() {
    this.environmentConfig.volumes.splice(this.selectedVolumeIndex, 1);
    this.selectedVolumeIndex = this.selectedVolumeIndex - 1;
  }

  addSecret() {
    const applicationSecret = new ApplicationSecret();
    applicationSecret.data = {};
    this.environmentConfig.secrets.push(applicationSecret);
    this.selectedSecretIndex = this.environmentConfig.secrets.length - 1;
  }

  deleteSecret() {
    this.environmentConfig.secrets.splice(this.selectedSecretIndex, 1);
    this.selectedSecretIndex = this.selectedSecretIndex - 1;
  }

  secretInUse(): boolean {
    const secret = this.environmentConfig.secrets[this.selectedSecretIndex];
    if (secret && secret.name) {
      const inUseInVolumes = this.environmentConfig.volumes
        .filter(volume => volume.type === 'Secret' && volume.secretName === secret.name).length > 0;
      const inUseInEnvironmentVariables = this.environmentConfig.environmentVariables
        .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
      return inUseInVolumes || inUseInEnvironmentVariables || this.environmentConfig.tlsSecretName === secret.name;
    }
    return false;
  }

  tlsSecrets(): ApplicationSecret[] {
    if (this.environmentConfig.secrets) {
      return this.environmentConfig.secrets.filter(secret => secret.type === 'Tls');
    }
    return [];
  }
}
