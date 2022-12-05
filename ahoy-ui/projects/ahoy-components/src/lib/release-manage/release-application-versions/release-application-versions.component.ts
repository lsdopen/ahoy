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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {filter, map, mergeMap, take} from 'rxjs/operators';
import {Application, ApplicationEnvironmentConfig, ApplicationVersion} from '../../applications/application';
import {ApplicationService} from '../../applications/application.service';
import {Confirmation} from '../../components/confirm-dialog/confirm';
import {DialogUtilService} from '../../components/dialog-util.service';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {Environment} from '../../environments/environment';
import {Release, ReleaseVersion, UpgradeAppOptions} from '../../releases/release';
import {ReleaseService} from '../../releases/release.service';
import {Role} from '../../util/auth';
import {AddApplicationDialogComponent} from '../add-application-dialog/add-application-dialog.component';
import {ReleaseManageService} from '../release-manage.service';
import {RouteHostnameResolver} from '../route-hostname-resolver';

@Component({
  selector: 'app-release-application-versions',
  templateUrl: './release-application-versions.component.html',
  styleUrls: ['./release-application-versions.component.scss']
})
export class ReleaseApplicationVersionsComponent implements OnInit {
  Role = Role;
  applicationVersions: ApplicationVersion[];
  @Input() environmentRelease: EnvironmentRelease;
  @Input() releaseVersion: ReleaseVersion;
  @Input() releaseChanged: EventEmitter<{ environmentRelease: EnvironmentRelease, releaseVersion: ReleaseVersion }>;
  @Output() applicationVersionsChanged = new EventEmitter();
  existingConfigs: Map<number, ApplicationEnvironmentConfig>;

  constructor(private releaseService: ReleaseService,
              private releaseManageService: ReleaseManageService,
              private applicationService: ApplicationService,
              private dialogService: DialogService,
              private dialogUtilService: DialogUtilService) {
  }

  ngOnInit() {
    if (this.releaseChanged) {
      this.releaseChanged.subscribe((data) => {
        this.environmentRelease = data.environmentRelease;
        this.releaseVersion = data.releaseVersion;
        this.getApplicationVersions();
      });
    }
  }

  getApplicationVersions() {
    this.applicationService.getAllVersionsForReleaseVersion(this.releaseVersion.id)
      .subscribe(applicationVersions => {
        this.applicationVersions = applicationVersions;
        this.getConfigs();
        this.getStatuses();
      });
  }

  getConfigs() {
    this.applicationService.getExistingEnvironmentConfigs(this.environmentRelease.id, this.releaseVersion.id)
      .subscribe((configs) => {
        this.existingConfigs = new Map<number, ApplicationEnvironmentConfig>();
        for (const conf of configs) {
          this.existingConfigs.set(conf.id.applicationVersionId, conf);
        }
      });
  }

  getStatuses() {
    this.applicationService.getApplicationReleaseStatus(this.environmentRelease.id, this.releaseVersion.id)
      .subscribe((statuses) => {
        for (const appVersion of this.applicationVersions) {
          for (const status of statuses) {
            if (status.id.applicationVersionId === appVersion.id) {
              appVersion.status = status;
            }
          }
        }
      });
  }

  public hasConfig(applicationVersionId: number): boolean {
    return this.existingConfigs.has(applicationVersionId);
  }

  addApplication() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Add application to ${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} in ${(this.environmentRelease.environment as Environment).key}:`;
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion, applicationVersions: this.applicationVersions};
    // TODO nested subscribes
    const dialogRef = this.dialogService.open(AddApplicationDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined) // cancelled
    ).subscribe((upgradeAppOptions: UpgradeAppOptions) => {
      this.releaseService.associateApplication(this.releaseVersion.id, upgradeAppOptions.applicationVersion.id)
        .subscribe(() => {
          this.applicationVersionsChanged.next(null);
        });
    });
  }

  removeApplication(applicationVersion: ApplicationVersion) {
    const confirmation = new Confirmation(`Are you sure you want to remove ${(applicationVersion.application as Application).name} from ` +
      `${(this.environmentRelease.release as Release).name}?`);
    confirmation.verify = true;
    confirmation.verifyText = (applicationVersion.application as Application).name;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.releaseService.removeAssociatedApplication(this.releaseVersion.id, applicationVersion.id)
        .subscribe(() => {
            this.applicationVersionsChanged.next(null);
          }
        );
    });
  }

  upgradeApplication(currentAppVersion: ApplicationVersion) {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Upgrade ${(currentAppVersion.application as Application).name}:${currentAppVersion.version} version to:`;
    dialogConfig.data = {
      environmentRelease: this.environmentRelease,
      releaseVersion: this.releaseVersion,
      applicationVersions: this.applicationVersions,
      currentApplicationVersion: currentAppVersion
    };

    let upgradeAppOptions: UpgradeAppOptions;
    const dialogRef = this.dialogService.open(AddApplicationDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      map((options: UpgradeAppOptions) => upgradeAppOptions = options),
      mergeMap(() => this.releaseService.removeAssociatedApplication(this.releaseVersion.id, currentAppVersion.id)),
      mergeMap(() => this.releaseService.associateApplication(this.releaseVersion.id, upgradeAppOptions.applicationVersion.id)),
      take(1),
      filter(() => upgradeAppOptions.copyEnvironmentConfig), // copy environment config?
      mergeMap(() => this.releaseManageService.copyAppEnvConfig(this.releaseVersion.id, currentAppVersion.id, upgradeAppOptions.applicationVersion.id)),
    ).subscribe({complete: () => this.getApplicationVersions()});
  }

  hasRoute(applicationVersion: ApplicationVersion): boolean {
    const config = this.existingConfigs.get(applicationVersion.id);
    return !!(config && config.spec.routeEnabled && config.spec.routes.length > 0);
  }

  getRoutes(applicationVersion: ApplicationVersion): any {
    const config = this.existingConfigs.get(applicationVersion.id);
    const routes = [];
    for (const route of config.spec.routes) {
      const resolvedHostname = RouteHostnameResolver.resolve(this.environmentRelease, applicationVersion, route.hostname);
      routes.push({hostname: resolvedHostname, url: config.spec.tls ? `https://${resolvedHostname}` : `http://${resolvedHostname}`});
    }
    return routes;
  }
}
