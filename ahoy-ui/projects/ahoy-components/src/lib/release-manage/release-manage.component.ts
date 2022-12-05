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
import {Component, EventEmitter, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {MenuItem} from 'primeng/api';
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {Observable, of, Subscription} from 'rxjs';
import {filter, mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Cluster} from '../clusters/cluster';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {DialogUtilService} from '../components/dialog-util.service';
import {DeployOptions, EnvironmentRelease, UndeployOptions} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {Environment} from '../environments/environment';
import {EnvironmentService} from '../environments/environment.service';
import {PromoteOptions, Release, ReleaseVersion, UpgradeOptions} from '../releases/release';
import {TaskEvent} from '../taskevents/task-events';
import {Role} from '../util/auth';
import {AuthService} from '../util/auth.service';
import {LoggerService} from '../util/logger.service';
import {CopyEnvironmentConfigDialogComponent} from './copy-environment-config-dialog/copy-environment-config-dialog.component';
import {PromoteDialogComponent} from './promote-dialog/promote-dialog.component';
import {RecentReleasesService} from './recent-releases.service';
import {ReleaseManageService} from './release-manage.service';
import {UpgradeDialogComponent} from './upgrade-dialog/upgrade-dialog.component';
import {ProgressMessages} from '../task/task';

@Component({
  selector: 'app-release-manage',
  templateUrl: './release-manage.component.html',
  styleUrls: ['./release-manage.component.scss']
})
export class ReleaseManageComponent implements OnInit, OnDestroy {
  Role = Role;
  environmentReleases: EnvironmentRelease[];
  releaseChanged = new EventEmitter<{ environmentRelease: EnvironmentRelease, releaseVersion: ReleaseVersion }>();
  environmentRelease: EnvironmentRelease;
  selectedEnvironmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  menuItems: MenuItem[];
  selectedReleaseVersion: ReleaseVersion;
  private paramMapSubscription: Subscription;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private releaseManageService: ReleaseManageService,
              private environmentService: EnvironmentService,
              private environmentReleaseService: EnvironmentReleaseService,
              private log: LoggerService,
              private location: Location,
              private dialogUtilService: DialogUtilService,
              private dialogService: DialogService,
              private breadcrumbService: AppBreadcrumbService,
              private recentReleasesService: RecentReleasesService,
              private authService: AuthService) {
  }

  ngOnInit() {
    this.paramMapSubscription = this.route.paramMap.subscribe((params) => {
      const paramMap = this.route.snapshot.paramMap;
      const environmentId = +paramMap.get('environmentId');
      const releaseId = +paramMap.get('releaseId');
      const releaseVersionId = +paramMap.get('releaseVersionId');
      this.getEnvironmentRelease(environmentId, releaseId, releaseVersionId).subscribe((environmentRelease) => {
        this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
      });
    });
  }

  ngOnDestroy(): void {
    if (this.paramMapSubscription) {
      this.paramMapSubscription.unsubscribe();
    }
  }

  private getEnvironmentRelease(environmentId: number, releaseId: number, releaseVersionId: number): Observable<EnvironmentRelease> {
    return this.environmentReleaseService.get(environmentId, releaseId).pipe(
      mergeMap(environmentRelease => {
        this.recentReleasesService.recent(environmentRelease, releaseVersionId);

        this.environmentRelease = environmentRelease;
        this.selectedEnvironmentRelease = environmentRelease;

        this.releaseVersion = (this.environmentRelease.release as Release).releaseVersions
          .find(relVersion => relVersion.id === releaseVersionId);

        this.setBreadcrumb();

        return of(environmentRelease);
      }),
      mergeMap(environmentRelease => {
          return this.environmentReleaseService.getReleasesByRelease((environmentRelease.release as Release).id);
        }
      ),
      mergeMap(environmentReleases => {
        this.environmentReleases = environmentReleases;
        this.setupMenuItems();

        this.selectedReleaseVersion = this.releaseVersion;

        return of(this.environmentRelease);
      })
    );
  }

  private setupMenuItems() {
    this.menuItems = [
      {
        label: 'History', icon: 'pi pi-fw pi-list',
        routerLink: `/releasehistory/${this.environmentRelease.id.releaseId}`
      },
      {
        label: 'Copy environment config', icon: 'pi pi-fw pi-copy', disabled: !this.canCopyEnvConfig(), visible: this.authService.hasOneOfRole([Role.admin, Role.releasemanager]),
        command: () => this.copyEnvConfig()
      }
    ];
  }

  private setBreadcrumb() {
    const env = (this.environmentRelease.environment as Environment);
    const rel = (this.environmentRelease.release as Release);
    this.breadcrumbService.setItems([
      {label: env.key, routerLink: '/environments'},
      {label: rel.name},
      {label: this.releaseVersion.version}
    ]);
  }

  reload(environmentRelease: EnvironmentRelease, releaseVersion: ReleaseVersion) {
    this.router.navigate(['/release', environmentRelease.id.environmentId, environmentRelease.id.releaseId, 'version', releaseVersion.id]);
  }

  canDeploy(): boolean {
    return !this.isCurrent();
  }

  canRedeploy(): boolean {
    return this.isCurrent();
  }

  canUndeploy(): boolean {
    return this.isCurrent();
  }

  deploy() {
    const commitMessage = `Deployed ` +
      `${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} to ` +
      `${(this.environmentRelease.environment as Environment).key} in ` +
      `${((this.environmentRelease.environment as Environment).cluster as Cluster).name}`;

    const confirmation = new Confirmation('Please enter a commit message:');
    confirmation.title = 'Deploy';
    confirmation.requiresInput = true;
    confirmation.input = commitMessage;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe((conf) => {
      const relToEnv = `${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} to environment ${(this.environmentRelease.environment as Environment).key}`;
      const deployOptions = new DeployOptions(this.releaseVersion.id, conf.input,
        new ProgressMessages(
          `Deploying ${relToEnv}`,
          `Deployed ${relToEnv}`,
          `Failed to deploy ${relToEnv}`
        ));
      this.releaseManageService.deploy(this.environmentRelease, this.releaseVersion, deployOptions).subscribe();
    });
  }

  undeploy() {
    const environmentKey = (this.environmentRelease.environment as Environment).key;
    const releaseName = (this.environmentRelease.release as Release).name;
    const confirmation = new Confirmation(`Are you sure you want to undeploy ${releaseName}:${this.releaseVersion.version} from ` +
      `${environmentKey}?`);
    confirmation.verify = true;
    confirmation.verifyText = `${environmentKey}/${releaseName}`;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      const relFromEnv = `${releaseName}:${this.releaseVersion.version} from environment ${environmentKey}`;
      const undeployOptions = new UndeployOptions(
        new ProgressMessages(
          `Undeploying ${relFromEnv}`,
          `Undeployed ${relFromEnv}`,
          `Failed to undeploy ${relFromEnv}`
        ));

      this.releaseManageService.undeploy(this.environmentRelease, undeployOptions).subscribe();
    });
  }

  canPromote() {
    return true;
  }

  promote() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Promote ${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} to:`;
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion};

    const dialogRef = this.dialogService.open(PromoteDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((promoteOptions: PromoteOptions) => {
        return this.releaseManageService.promote(this.environmentRelease.id, promoteOptions);
      })
    ).subscribe((newEnvironmentRelease: EnvironmentRelease) => this.reload(newEnvironmentRelease, this.releaseVersion));
  }

  canUpgrade() {
    return true;
  }

  upgrade() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Upgrade ${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} to version:`;
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion};

    const dialogRef = this.dialogService.open(UpgradeDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((upgradeOptions: UpgradeOptions) => {
        return this.releaseManageService.upgrade(this.environmentRelease, this.releaseVersion, upgradeOptions);
      })
    ).subscribe((newReleaseVersion: ReleaseVersion) => this.reload(this.environmentRelease, newReleaseVersion));
  }

  canCopyEnvConfig() {
    return true;
  }

  copyEnvConfig() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Copy environment config for version ${this.releaseVersion.version} from:`;
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion};

    const dialogRef = this.dialogService.open(CopyEnvironmentConfigDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((selectedReleaseVersion) => {
        return this.releaseManageService.copyEnvConfig(this.environmentRelease.id, selectedReleaseVersion.id, this.releaseVersion.id);
      })
    ).subscribe(() => this.reload(this.environmentRelease, this.releaseVersion));
  }

  releaseVersionChanged() {
    this.reload(this.environmentRelease, this.selectedReleaseVersion);
  }

  private isCurrent() {
    return this.environmentRelease.currentReleaseVersion
      && this.releaseVersion.version === this.environmentRelease.currentReleaseVersion.version;
  }

  canRollback() {
    return this.environmentRelease.previousReleaseVersion;
  }

  rollback() {
    const commitMessage = `Rolled back deployment ` +
      `${(this.environmentRelease.release as Release).name}:${this.environmentRelease.currentReleaseVersion.version} to ` +
      `${(this.environmentRelease.release as Release).name}:${this.environmentRelease.previousReleaseVersion.version} in ` +
      `${(this.environmentRelease.environment as Environment).key} in ` +
      `${((this.environmentRelease.environment as Environment).cluster as Cluster).name}`;

    const confirmation = new Confirmation('Please enter a commit message:');
    confirmation.title = 'Rollback';
    confirmation.requiresInput = true;
    confirmation.input = commitMessage;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe((conf) => {
      const xToY = `${(this.environmentRelease.release as Release).name}:${this.environmentRelease.currentReleaseVersion.version} to ${(this.environmentRelease.release as Release).name}:${this.environmentRelease.previousReleaseVersion.version}`;
      const deployOptions = new DeployOptions(this.environmentRelease.previousReleaseVersion.id, conf.input,
        new ProgressMessages(
          `Rolling back ${xToY}`,
          `Rolled back ${xToY}`,
          `Failed to roll back ${xToY}`
        ));
      this.releaseManageService.deploy(this.environmentRelease, this.environmentRelease.previousReleaseVersion, deployOptions)
        .subscribe(() => this.log.debug('rolled back release:', this.environmentRelease));
    });
  }

  canShowResources() {
    return this.environmentRelease.deployed &&
      this.releaseVersion.id === this.environmentRelease.currentReleaseVersion.id;
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.environmentRelease.id.releaseId === statusChangedEvent.environmentReleaseId.releaseId) {
        setTimeout(() => {
          this.getEnvironmentRelease(this.environmentRelease.id.environmentId, this.environmentRelease.id.releaseId, this.releaseVersion.id)
            .subscribe(() => {
              if (this.releaseVersion.id === statusChangedEvent.releaseVersionId) {
                this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
              }
            });
        }, 1000);
      }
    }
  }

  tableSelectionChanged() {
    if (this.selectedEnvironmentRelease) {
      this.reload(this.selectedEnvironmentRelease, this.releaseVersion);
    }
  }

  applicationVersionsChanged() {
    this.getEnvironmentRelease(this.environmentRelease.id.environmentId, this.environmentRelease.id.releaseId, this.releaseVersion.id).subscribe(() => {
      this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
    });
  }
}
