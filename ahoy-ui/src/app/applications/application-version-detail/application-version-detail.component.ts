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
import {filter, map, mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {TabItemFactory} from '../../components/multi-tab/multi-tab.component';
import {ReleaseManageService} from '../../release-manage/release-manage.service';
import {ReleaseService} from '../../releases/release.service';
import {Application, ApplicationSecret, ApplicationSpec, ApplicationVersion, ApplicationVolume} from '../application';
import {ApplicationService} from '../application.service';

@Component({
  selector: 'app-application-version-detail',
  templateUrl: './application-version-detail.component.html',
  styleUrls: ['./application-version-detail.component.scss']
})
export class ApplicationVersionDetailComponent implements OnInit {
  private releaseVersionId: number;
  private applicationVersionId: number;
  private copyEnvironmentConfig: boolean;
  application: Application;
  applicationVersion: ApplicationVersion;
  editMode: boolean;
  editingVersion: string;
  newServicePort: number;
  portsCategory = false;
  environmentVariablesCategory = false;
  configFilesCategory = false;
  volumesCategory = false;
  secretsCategory = false;
  resourcesCategory = false;
  newArg: string;
  editingArg: string;
  editingPort: number;

  constructor(private route: ActivatedRoute,
              private applicationService: ApplicationService,
              private releaseService: ReleaseService,
              private releaseManageService: ReleaseManageService,
              private location: Location,
              private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const applicationId = +this.route.snapshot.paramMap.get('appId');
    this.releaseVersionId = +this.route.snapshot.queryParamMap.get('releaseVersionId');
    this.applicationVersionId = +this.route.snapshot.queryParamMap.get('applicationVersionId');
    this.copyEnvironmentConfig = JSON.parse(this.route.snapshot.queryParamMap.get('copyEnvironmentConfig'));

    // TODO nested subscribes
    this.applicationService.get(applicationId)
      .subscribe(application => {
        this.application = application;

        const versionId = this.route.snapshot.paramMap.get('versionId');
        if (versionId === 'new') {
          this.editMode = false;
          this.applicationVersion = new ApplicationVersion();

          this.setBreadcrumb();
          // load previous version details for convenience
          if (this.applicationVersionId && this.applicationVersionId > 0) {
            this.applicationService.getVersion(this.applicationVersionId)
              .subscribe((applicationVersion) => {
                this.applicationVersion.spec = applicationVersion.spec;

                this.setCategoriesExpanded();
              });

          } else {
            this.applicationVersion.spec = new ApplicationSpec();
          }

        } else {
          this.editMode = true;
          this.applicationService.getVersion(+versionId)
            .subscribe(applicationVersion => {
              this.applicationVersion = applicationVersion;
              this.editingVersion = applicationVersion.version;

              if (!this.applicationVersion.spec.healthEndpointScheme) {
                this.applicationVersion.spec.healthEndpointScheme = 'HTTP';
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
    if (this.applicationVersion.spec.servicePorts && this.applicationVersion.spec.servicePorts.length > 0) {
      this.portsCategory = true;
    }

    if (this.applicationVersion.spec.environmentVariables && Object.keys(this.applicationVersion.spec.environmentVariables).length > 0) {
      this.environmentVariablesCategory = true;
    }

    if (this.applicationVersion.spec.configPath) {
      this.configFilesCategory = true;
    }

    if (this.applicationVersion.spec.volumes.length > 0) {
      this.volumesCategory = true;
    }

    if (this.applicationVersion.spec.secrets.length > 0) {
      this.secretsCategory = true;
    }

    if (this.applicationVersion.spec.resources && (
      this.applicationVersion.spec.resources.limitCpu || this.applicationVersion.spec.resources.limitMemory ||
      this.applicationVersion.spec.resources.requestCpu || this.applicationVersion.spec.resources.requestMemory)) {
      this.resourcesCategory = true;
    }
  }

  save() {
    this.applicationVersion.application = this.applicationService.link(this.application.id);
    let savedApplicationVersion;
    this.applicationService.saveVersion(this.applicationVersion).pipe(
      map((applicationVersion) => {
        savedApplicationVersion = applicationVersion;
        return applicationVersion;
      }),
      // are we upgrading the application version for a release version? therefore changing association from existing to new app version
      filter(() => !!(this.releaseVersionId && this.applicationVersionId)),
      mergeMap(() => this.releaseService.removeAssociatedApplication(this.releaseVersionId, this.applicationVersionId)),
      mergeMap(() => this.releaseService.associateApplication(this.releaseVersionId, savedApplicationVersion.id)),
      // copy environment config?
      filter(() => this.copyEnvironmentConfig),
      mergeMap(() => this.releaseManageService.copyAppEnvConfig(this.releaseVersionId, this.applicationVersionId, savedApplicationVersion.id)),
    ).subscribe({complete: () => this.location.back()});
  }

  cancel() {
    this.location.back();
  }

  addServicePort() {
    if (this.newServicePort) {
      this.applicationVersion.spec.servicePorts.push(this.newServicePort);
      this.newServicePort = null;
    }
  }

  removeServicePort(portIndex: number) {
    this.applicationVersion.spec.servicePorts.splice(portIndex, 1);
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
        const inUseInVolumes = this.applicationVersion.spec.volumes
          .filter(volume => volume.type === 'Secret' && volume.secretName === secret.name).length > 0;
        const inUseInEnvironmentVariables = this.applicationVersion.spec.environmentVariables
          .filter(envVar => envVar.type === 'Secret' && envVar.secretName === secret.name).length > 0;
        return inUseInVolumes || inUseInEnvironmentVariables;
      }
      return false;
    };
  }

  secretInUseTooltip() {
    return (secret: ApplicationSecret): string => {
      return 'Secret in use';
    };
  }

  addArg() {
    if (this.newArg) {
      if (!this.applicationVersion.spec.args) {
        this.applicationVersion.spec.args = [];
      }
      this.applicationVersion.spec.args.push(this.newArg);
      this.newArg = null;
    }
  }

  removeArg(argIndex: number) {
    this.applicationVersion.spec.args.splice(argIndex, 1);
  }

  editArgInit($event: any) {
    const index = $event.index;
    this.editingArg = this.applicationVersion.spec.args[index];
  }

  editArgComplete($event: any) {
    const index = $event.index;
    this.applicationVersion.spec.args[index] = this.editingArg;
  }

  editPortInit($event: any) {
    const index = $event.index;
    this.editingPort = this.applicationVersion.spec.servicePorts[index];
  }

  editPortComplete($event: any) {
    const index = $event.index;
    this.applicationVersion.spec.servicePorts[index] = this.editingPort;
  }
}
