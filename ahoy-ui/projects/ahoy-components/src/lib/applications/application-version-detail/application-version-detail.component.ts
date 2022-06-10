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
import {Application, ApplicationSecret, ApplicationSpec, ApplicationVersion, ApplicationVolume, ContainerSpec} from '../application';
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

  containerSpecFactory(): TabItemFactory<ContainerSpec> {
    return (): ContainerSpec => {
      return new ContainerSpec();
    };
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
        const containers = [this.applicationVersion.spec, ...this.applicationVersion.spec.containers];
        const inUseInEnvironmentVariables = containers
          .flatMap((containerSpec) => containerSpec.environmentVariables)
          .filter(envVar => {
            return envVar.type === 'Secret' && envVar.secretName === secret.name;
          }).length > 0;
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

  containerIcon() {
    return (containerSpec: ContainerSpec): string => {
      switch (containerSpec.type) {
        case 'Container':
          return 'pi pi-stop';
        case 'Init':
          return 'pi pi-window-maximize';
      }
    };
  }

  containerSpecDeleteDisabled() {
    return (containerSpec: ContainerSpec): boolean => {
      return containerSpec === this.applicationVersion.spec;
    };
  }

  containerSpecDeleteDisabledTooltip() {
    return (containerSpec: ContainerSpec): string => {
      return 'Cannot remove default container';
    };
  }
}
