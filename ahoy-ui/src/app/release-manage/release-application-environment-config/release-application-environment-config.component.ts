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

import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {mergeMap} from 'rxjs/operators';
import {Application, ApplicationEnvironmentConfig, ApplicationEnvironmentConfigId, ApplicationSecret, ApplicationVersion} from '../../applications/application';
import {ApplicationService} from '../../applications/application.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {Environment} from '../../environments/environment';
import {Release, ReleaseVersion} from '../../releases/release';
import {ReleaseService} from '../../releases/release.service';
import {LoggerService} from '../../util/logger.service';

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
  volumesCategory = false;
  secretsCategory = false;

  constructor(
    private log: LoggerService,
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private environmentReleaseService: EnvironmentReleaseService,
    private releasesService: ReleaseService,
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
    }

    if (this.environmentConfig.volumes.length > 0) {
      this.volumesCategory = true;
    }

    if (this.environmentConfig.secrets.length > 0) {
      this.secretsCategory = true;
    }
  }

  save() {
    this.applicationService.saveEnvironmentConfig(this.environmentConfig)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.location.back();
  }

  routeSelectedChange() {
    if (this.routeCategory && !this.environmentConfig.routeHostname) {
      this.environmentConfig.routeHostname = this.exampleRouteHost;
    }
  }

  tlsSecrets(): ApplicationSecret[] {
    if (this.environmentConfig.secrets) {
      return this.environmentConfig.secrets.filter(secret => secret.type === 'Tls');
    }
    return [];
  }
}
