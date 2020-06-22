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

import {Component, EventEmitter, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {LoggerService} from '../../util/logger.service';
import {EnvironmentService} from '../../environments/environment.service';
import {filter, flatMap} from 'rxjs/operators';
import {Observable, of, Subscription} from 'rxjs';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {PromoteDialogComponent} from '../promote-dialog/promote-dialog.component';
import {UpgradeDialogComponent} from '../upgrade-dialog/upgrade-dialog.component';
import {Release, ReleaseVersion} from '../release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {DeployDetails, EnvironmentRelease} from '../../environment-release/environment-release';
import {CopyEnvironmentConfigDialogComponent} from '../copy-environment-config-dialog/copy-environment-config-dialog.component';
import {TaskEvent} from '../../taskevents/task-events';
import {Environment} from '../../environments/environment';
import {Confirmation} from '../../components/confirm-dialog/confirm';
import {DialogService} from '../../components/dialog.service';
import {ReleaseService} from '../../release/release.service';

@Component({
  selector: 'app-release-manage',
  templateUrl: './release-manage.component.html',
  styleUrls: ['./release-manage.component.scss']
})
export class ReleaseManageComponent implements OnInit, OnDestroy {
  private environmentReleases: EnvironmentRelease[];
  private releaseChanged = new EventEmitter<{ environmentRelease: EnvironmentRelease, releaseVersion: ReleaseVersion }>();
  private environmentReleaseChangedSubscription: Subscription;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private releaseService: ReleaseService,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private log: LoggerService,
    private location: Location,
    private dialogService: DialogService,
    private dialog: MatDialog) {
  }

  ngOnInit() {
    const environmentId = +this.route.snapshot.paramMap.get('environmentId');
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');
    const releaseVersionId = +this.route.snapshot.paramMap.get('releaseVersionId');
    this.getEnvironmentRelease(environmentId, releaseId, releaseVersionId).subscribe((environmentRelease) => {
      this.subscribeToEnvironmentReleaseChanged();
    });
  }

  ngOnDestroy(): void {
    if (this.environmentReleaseChangedSubscription) {
      this.environmentReleaseChangedSubscription.unsubscribe();
    }
  }

  private getEnvironmentRelease(environmentId: number, releaseId: number, releaseVersionId: number): Observable<EnvironmentRelease> {
    return this.environmentReleaseService.get(environmentId, releaseId).pipe(
      flatMap(environmentRelease => {
        this.environmentRelease = environmentRelease;

        this.releaseVersion = (this.environmentRelease.release as Release).releaseVersions
          .find(relVersion => relVersion.id === releaseVersionId);

        return of(environmentRelease);
      }),
      flatMap(environmentRelease => {
          return this.environmentReleaseService.getReleasesByRelease((environmentRelease.release as Release).id);
        }
      ), flatMap(environmentReleases => {
        this.environmentReleases = environmentReleases;
        return of(this.environmentRelease);
      })
    );
  }

  private subscribeToEnvironmentReleaseChanged() {
    if (!this.environmentReleaseChangedSubscription) {
      this.environmentReleaseChangedSubscription = this.releaseService.environmentReleaseChanged()
        .subscribe((environmentRelease) => {
          if (EnvironmentReleaseService.environmentReleaseEquals(this.environmentRelease, environmentRelease)) {
            this.getEnvironmentRelease(environmentRelease.id.environmentId, environmentRelease.id.releaseId, this.releaseVersion.id)
              .subscribe(() => {
                this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
              });
          }
        });
    }
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
      `${(this.environmentRelease.environment as Environment).name} in ` +
      `${(this.environmentRelease.environment as Environment).cluster.name}`;

    const confirmation = new Confirmation('Please enter a commit message:');
    confirmation.title = 'Deploy';
    confirmation.requiresInput = true;
    confirmation.input = commitMessage;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe((conf) => {
      const deployDetails = new DeployDetails(conf.input);
      this.releaseService.deploy(this.environmentRelease, this.releaseVersion, deployDetails).subscribe();
    });
  }

  undeploy() {
    const confirmation = new Confirmation(`Are you sure you want to undeploy ${(this.environmentRelease.release as Release).name}:${this.releaseVersion.version} from ` +
      `${(this.environmentRelease.environment as Environment).name}?`);
    confirmation.verify = true;
    confirmation.verifyText = (this.environmentRelease.release as Release).name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.releaseService.undeploy(this.environmentRelease).subscribe();
    });
  }

  canPromote() {
    return true;
  }

  promote() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion};

    const dialogRef = this.dialog.open(PromoteDialogComponent, dialogConfig);
    dialogRef.afterClosed().pipe(
      filter((result) => result !== undefined), // cancelled
      flatMap((destEnvironment) => {
        return this.releaseService.promote(this.environmentRelease.id, destEnvironment.id);
      })
    ).subscribe((newEnvironmentRelease: EnvironmentRelease) => this.reload(newEnvironmentRelease.id.environmentId, this.releaseVersion.id));
  }

  canUpgrade() {
    return true;
  }

  upgrade() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = {environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion};

    const dialogRef = this.dialog.open(UpgradeDialogComponent, dialogConfig);
    dialogRef.afterClosed().pipe(
      filter((result) => result !== undefined), // cancelled
      flatMap((version) => {
        return this.releaseService.upgrade(this.releaseVersion.id, version);
      })
    ).subscribe((newReleaseVersion: ReleaseVersion) => this.reload(this.environmentRelease.id.environmentId, newReleaseVersion.id));
  }

  canEdit() {
    return !this.isCurrent();
  }

  canCopyEnvConfig() {
    return true;
  }

  copyEnvConfig() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = [this.environmentRelease, this.releaseVersion];

    const dialogRef = this.dialog.open(CopyEnvironmentConfigDialogComponent, dialogConfig);
    dialogRef.afterClosed().pipe(
      filter((result) => result !== undefined), // cancelled
      flatMap((selectedReleaseVersion) => {
        return this.releaseService.copyEnvConfig(this.environmentRelease.id, selectedReleaseVersion.id, this.releaseVersion.id);
      })
    ).subscribe(() => this.reload(this.environmentRelease.id.environmentId, this.releaseVersion.id));
  }

  releaseVersionChanged() {
    this.router.navigate(
      ['/release', this.environmentRelease.id.environmentId, this.environmentRelease.id.releaseId, 'version', this.releaseVersion.id]);
    this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
  }

  reload(environmentId: number, releaseVersionId: number) {
    this.router.navigate(
      ['/release', environmentId, this.environmentRelease.id.releaseId, 'version', releaseVersionId])
      .then(() => {
        this.getEnvironmentRelease(environmentId, this.environmentRelease.id.releaseId, releaseVersionId)
          .subscribe(() => {
            this.releaseChanged.emit({environmentRelease: this.environmentRelease, releaseVersion: this.releaseVersion});
          });
      });
  }

  private isCurrent() {
    return this.environmentRelease.currentReleaseVersion
      && this.releaseVersion.version === this.environmentRelease.currentReleaseVersion.version;
  }

  compareReleaseVersions(r1: ReleaseVersion, r2: ReleaseVersion): boolean {
    if (r1 === null) {
      return r2 === null;
    }

    if (r2 === null) {
      return r1 === null;
    }

    return r1.version === r2.version;
  }

  canRollback() {
    return this.environmentRelease.previousReleaseVersion;
  }

  rollback() {
    const commitMessage = `Rolled back deployment ` +
      `${(this.environmentRelease.release as Release).name}:${this.environmentRelease.currentReleaseVersion.version} to ` +
      `${(this.environmentRelease.release as Release).name}:${this.environmentRelease.previousReleaseVersion.version} in ` +
      `${(this.environmentRelease.environment as Environment).name} in ` +
      `${(this.environmentRelease.environment as Environment).cluster.name}`;

    const confirmation = new Confirmation('Please enter a commit message:');
    confirmation.title = 'Rollback';
    confirmation.requiresInput = true;
    confirmation.input = commitMessage;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe((conf) => {
      const deployDetails = new DeployDetails(conf.input);
      this.releaseService.deploy(this.environmentRelease, this.environmentRelease.previousReleaseVersion, deployDetails)
        .subscribe(() => this.log.debug('rolled back release:', this.environmentRelease));
    });
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (EnvironmentReleaseService.environmentReleaseIdEquals(this.environmentRelease.id, statusChangedEvent.environmentReleaseId)) {
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

  isCurrentEnvironmentRelease(environmentRelease: EnvironmentRelease) {
    return EnvironmentReleaseService.environmentReleaseEquals(this.environmentRelease, environmentRelease);
  }
}
