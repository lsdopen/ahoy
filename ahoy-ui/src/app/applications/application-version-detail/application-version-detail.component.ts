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
import {ActivatedRoute} from '@angular/router';
import {ApplicationService} from '../application.service';
import {LoggerService} from '../../util/logger.service';
import {Application, ApplicationConfig, ApplicationSecret, ApplicationVersion, ApplicationVolume} from '../application';
import {Location} from '@angular/common';
import {ReleasesService} from '../../releases/releases.service';
import {MatTableDataSource} from '@angular/material/table';

@Component({
  selector: 'app-application-version-detail',
  templateUrl: './application-version-detail.component.html',
  styleUrls: ['./application-version-detail.component.scss']
})
export class ApplicationVersionDetailComponent implements OnInit {
  private releaseVersionId: number;
  private applicationVersionId: number;
  application: Application;
  applicationVersion: ApplicationVersion;
  editMode: boolean;
  editingVersion: string;
  newServicePort: number;
  portsDataSource = new MatTableDataSource<number>();
  portsDisplayedColumns = ['port', 'remove'];
  portsCategory = false;
  healthChecksCategory = false;
  environmentVariablesCategory = false;
  configFilesCategory = false;
  selectedConfigIndex: number;
  volumesCategory = false;
  selectedVolumeIndex: number;
  secretsCategory = false;
  selectedSecretIndex: number;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private releasesService: ReleasesService,
    private location: Location,
    private log: LoggerService) {
  }

  ngOnInit() {
    const applicationId = +this.route.snapshot.paramMap.get('appId');
    this.releaseVersionId = +this.route.snapshot.queryParamMap.get('releaseVersionId');
    this.applicationVersionId = +this.route.snapshot.queryParamMap.get('applicationVersionId');

    this.applicationService.get(applicationId)
      .subscribe(application => {
        this.application = application;

        const versionId = this.route.snapshot.paramMap.get('versionId');
        if (versionId === 'new') {
          this.editMode = false;
          this.applicationVersion = new ApplicationVersion();
          this.applicationVersion.configs = [];
          this.applicationVersion.volumes = [];
          this.applicationVersion.secrets = [];
          this.applicationVersion.servicePorts = [];
          this.applicationVersion.environmentVariables = [];
          this.applicationVersion.healthEndpointScheme = 'HTTP';
          this.portsDataSource.data = this.applicationVersion.servicePorts;

          // load previous version details for convenience
          if (this.applicationVersionId && this.applicationVersionId > 0) {
            this.applicationService.getVersion(this.applicationVersionId)
              .subscribe((applicationVersion) => {
                this.applicationVersion.dockerRegistry = applicationVersion.dockerRegistry;
                this.applicationVersion.image = applicationVersion.image;

                this.applicationVersion.environmentVariables = applicationVersion.environmentVariables;

                this.applicationVersion.servicePorts = applicationVersion.servicePorts;
                this.portsDataSource.data = this.applicationVersion.servicePorts;

                this.applicationVersion.healthEndpointPath = applicationVersion.healthEndpointPath;
                this.applicationVersion.healthEndpointPort = applicationVersion.healthEndpointPort;
                this.applicationVersion.healthEndpointScheme = applicationVersion.healthEndpointScheme;

                this.applicationVersion.configs = this.cloneConfigs(applicationVersion.configs);
                this.applicationVersion.volumes = this.cloneVolumes(applicationVersion.volumes);
                this.applicationVersion.secrets = this.cloneSecrets(applicationVersion.secrets);
                this.applicationVersion.configPath = applicationVersion.configPath;

                this.setCategoriesExpanded();
              });
          }

        } else {
          this.editMode = true;
          this.applicationService.getVersion(+versionId)
            .subscribe(applicationVersion => {
              this.applicationVersion = applicationVersion;
              this.editingVersion = applicationVersion.version;

              if (!this.applicationVersion.healthEndpointScheme) {
                this.applicationVersion.healthEndpointScheme = 'HTTP';
              }
              this.portsDataSource.data = this.applicationVersion.servicePorts;

              this.setCategoriesExpanded();
            });
        }
      });
  }

  private setCategoriesExpanded() {
    if (this.applicationVersion.servicePorts && this.applicationVersion.servicePorts.length > 0) {
      this.portsCategory = true;
    }

    if (this.applicationVersion.healthEndpointPath) {
      this.healthChecksCategory = true;
    }

    if (this.applicationVersion.environmentVariables && Object.keys(this.applicationVersion.environmentVariables).length > 0) {
      this.environmentVariablesCategory = true;
    }

    if (this.applicationVersion.configPath) {
      this.configFilesCategory = true;
      this.selectedConfigIndex = 0;
    }

    if (this.applicationVersion.volumes.length > 0) {
      this.volumesCategory = true;
      this.selectedVolumeIndex = 0;
    }

    if (this.applicationVersion.secrets.length > 0) {
      this.secretsCategory = true;
      this.selectedSecretIndex = 0;
    }
  }

  save() {
    this.applicationVersion.application = this.applicationService.link(this.application.id);
    this.applicationService.saveVersion(this.applicationVersion)
      .subscribe((applicationVersion) => {
        if (this.releaseVersionId && this.applicationVersionId) {
          this.releasesService.removeAssociatedApplication(this.releaseVersionId, this.applicationVersionId)
            .subscribe(() => {
              this.releasesService.associateApplication(this.releaseVersionId, applicationVersion.id)
                .subscribe(() => this.location.back());
            });
        } else {
          this.location.back();
        }
      });
  }

  cancel() {
    this.location.back();
  }

  addConfig() {
    this.applicationVersion.configs.push(new ApplicationConfig());
    this.selectedConfigIndex = this.applicationVersion.configs.length - 1;
  }

  deleteConfig() {
    this.applicationVersion.configs.splice(this.selectedConfigIndex, 1);
    this.selectedConfigIndex = this.selectedConfigIndex - 1;
  }

  private cloneConfigs(configs: ApplicationConfig[]): ApplicationConfig[] {
    return configs.map((applicationConfig) => {
      const appConfig = new ApplicationConfig();
      appConfig.name = applicationConfig.name;
      appConfig.config = applicationConfig.config;
      return appConfig;
    });
  }

  addServicePort() {
    if (this.newServicePort) {
      this.applicationVersion.servicePorts.push(this.newServicePort);
      this.portsDataSource.data = this.applicationVersion.servicePorts;
      this.newServicePort = null;
    }
  }

  removeServicePort(portIndex: number) {
    this.applicationVersion.servicePorts.splice(portIndex, 1);
    this.portsDataSource.data = this.applicationVersion.servicePorts;
  }

  addVolume() {
    this.applicationVersion.volumes.push(new ApplicationVolume());
    this.selectedVolumeIndex = this.applicationVersion.volumes.length - 1;
  }

  deleteVolume() {
    this.applicationVersion.volumes.splice(this.selectedVolumeIndex, 1);
    this.selectedVolumeIndex = this.selectedVolumeIndex - 1;
  }

  private cloneVolumes(volumes: ApplicationVolume[]): ApplicationVolume[] {
    return volumes.map((applicationVolume) => {
      const appVolume = new ApplicationVolume();
      appVolume.name = applicationVolume.name;
      appVolume.mountPath = applicationVolume.mountPath;
      appVolume.storageClassName = applicationVolume.storageClassName;
      appVolume.accessMode = applicationVolume.accessMode;
      appVolume.size = applicationVolume.size;
      appVolume.sizeStorageUnit = applicationVolume.sizeStorageUnit;
      return appVolume;
    });
  }

  addSecret() {
    let applicationSecret = new ApplicationSecret();
    applicationSecret.type = 'Generic';
    applicationSecret.data = {};
    this.applicationVersion.secrets.push(applicationSecret);
    this.selectedSecretIndex = this.applicationVersion.secrets.length - 1;
  }

  deleteSecret() {
    this.applicationVersion.secrets.splice(this.selectedSecretIndex, 1);
    this.selectedSecretIndex = this.selectedSecretIndex - 1;
  }

  secretInUse(): boolean {
    let secret = this.applicationVersion.secrets[this.selectedSecretIndex];
    if (secret && secret.name) {
      let inUseInVolumes = this.applicationVersion.volumes
        .filter(volume => volume.type === 'Secret' && volume.secretName === secret.name).length > 0;
      let inUseInEnvironmentVariables = this.applicationVersion.environmentVariables
        .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
      return inUseInVolumes || inUseInEnvironmentVariables;
    }
    return false;
  }

  private cloneSecrets(secrets: ApplicationSecret[]): ApplicationSecret[] {
    return secrets.map((applicationSecret) => {
      const appSecret = new ApplicationSecret();
      appSecret.name = applicationSecret.name;
      appSecret.data = applicationSecret.data;
      return appSecret;
    });
  }
}
