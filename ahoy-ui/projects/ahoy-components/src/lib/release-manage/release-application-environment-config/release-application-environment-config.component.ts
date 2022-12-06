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

import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {Application, ApplicationEnvironmentConfig, ApplicationEnvironmentConfigId, ApplicationSecret, ApplicationVersion, ApplicationVolume} from '../../applications/application';
import {ApplicationService} from '../../applications/application.service';
import {TabItemFactory} from '../../components/multi-tab/multi-tab.component';
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
  environmentConfig: ApplicationEnvironmentConfig;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  applicationVersion: ApplicationVersion;

  constructor(private log: LoggerService,
              private route: ActivatedRoute,
              private applicationService: ApplicationService,
              private environmentReleaseService: EnvironmentReleaseService,
              private releasesService: ReleaseService,
              private location: Location,
              private breadcrumbService: AppBreadcrumbService) {
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
          return this.releasesService.getVersionSummary(releaseVersionId);
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
        this.setBreadcrumb();
      });
  }

  private setBreadcrumb() {
    const env = (this.environmentRelease.environment as Environment);
    const rel = (this.environmentRelease.release as Release);
    const app = (this.applicationVersion.application as Application);
    this.breadcrumbService.setItems([
      {label: env.key, routerLink: '/environments'},
      {label: rel.name, routerLink: `/release/${env.id}/${rel.id}/version/${this.releaseVersion.id}`},
      {label: this.releaseVersion.version, routerLink: `/release/${env.id}/${rel.id}/version/${this.releaseVersion.id}`},
      {label: app.name},
      {label: this.applicationVersion.version},
      {label: 'env config'}
    ]);
  }

  save() {
    this.applicationService.saveEnvironmentConfig(this.environmentConfig)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.location.back();
  }

  tlsSecrets(): ApplicationSecret[] {
    if (this.environmentConfig.spec.secrets) {
      return this.environmentConfig.spec.secrets.filter(secret => secret.type === 'Tls');
    }
    return [];
  }

  applicationVolumeFactory(): TabItemFactory<ApplicationVolume> {
    return (): ApplicationVolume => {
      return new ApplicationVolume();
    };
  }

  applicationSecretFactory(): TabItemFactory<ApplicationSecret> {
    return (): ApplicationSecret => {
      const applicationSecret = new ApplicationSecret();
      applicationSecret.type = 'Generic';
      applicationSecret.data = {};
      return applicationSecret;
    };
  }

  secretInUse() {
    return (secret: ApplicationSecret): boolean => {
      if (secret && secret.name) {
        const inUseInVolumes = this.environmentConfig.spec.volumes
          .filter(volume => volume.type === 'Secret' && volume.secretName === secret.name).length > 0;
        const inUseInEnvironmentVariables = this.environmentConfig.spec.environmentVariables
          .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
        return inUseInVolumes || inUseInEnvironmentVariables ||
          (this.environmentConfig.spec.tlsSecretName !== undefined ? this.environmentConfig.spec.tlsSecretName === secret.name : false);
      }
      return false;
    };
  }

  secretInUseTooltip() {
    return (secret: ApplicationSecret): string => {
      return 'Secret in use';
    };
  }
}
