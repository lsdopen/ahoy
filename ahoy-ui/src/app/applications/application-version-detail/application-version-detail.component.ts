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
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {ReleaseService} from '../../releases/release.service';
import {Application, ApplicationConfig, ApplicationSecret, ApplicationVersion, ApplicationVolume} from '../application';
import {ApplicationService} from '../application.service';

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
  portsCategory = false;
  healthChecksCategory = false;
  environmentVariablesCategory = false;
  configFilesCategory = false;
  volumesCategory = false;
  secretsCategory = false;

  constructor(private route: ActivatedRoute,
              private applicationService: ApplicationService,
              private releasesService: ReleaseService,
              private location: Location,
              private breadcrumbService: AppBreadcrumbService) {
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

          this.setBreadcrumb();
          // load previous version details for convenience
          if (this.applicationVersionId && this.applicationVersionId > 0) {
            this.applicationService.getVersion(this.applicationVersionId)
              .subscribe((applicationVersion) => {
                this.applicationVersion.dockerRegistry = applicationVersion.dockerRegistry;
                this.applicationVersion.image = applicationVersion.image;

                this.applicationVersion.environmentVariables = applicationVersion.environmentVariables;

                this.applicationVersion.servicePorts = applicationVersion.servicePorts;

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

              this.setCategoriesExpanded();
              this.setBreadcrumb();
            });
        }
      });
  }

  private setBreadcrumb() {
    if (this.editMode) {
      this.breadcrumbService.setItems([
        {label: 'applications', routerLink: '/applications'},
        {label: this.application.name, routerLink: `/application/${this.application.id}`},
        {label: this.applicationVersion.version},
        {label: 'edit'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'applications', routerLink: '/applications'},
        {label: this.application.name, routerLink: `/application/${this.application.id}`},
        {label: 'new'}
      ]);
    }
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
    }

    if (this.applicationVersion.volumes.length > 0) {
      this.volumesCategory = true;
    }

    if (this.applicationVersion.secrets.length > 0) {
      this.secretsCategory = true;
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
      this.newServicePort = null;
    }
  }

  removeServicePort(portIndex: number) {
    this.applicationVersion.servicePorts.splice(portIndex, 1);
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

  private cloneSecrets(secrets: ApplicationSecret[]): ApplicationSecret[] {
    return secrets.map((applicationSecret) => {
      const appSecret = new ApplicationSecret();
      appSecret.name = applicationSecret.name;
      appSecret.data = applicationSecret.data;
      return appSecret;
    });
  }
}
