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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConfirmationService} from 'primeng/api';
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {Observable, of} from 'rxjs';
import {filter, mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Environment} from '../environments/environment';
import {EnvironmentService} from '../environments/environment.service';
import {ReleaseManageService} from '../release-manage/release-manage.service';
import {Release} from '../releases/release';
import {ReleaseService} from '../releases/release.service';
import {TaskEvent} from '../taskevents/task-events';
import {Role} from '../util/auth';
import {LoggerService} from '../util/logger.service';
import {AddReleaseDialogComponent} from './add-release-dialog/add-release-dialog.component';
import {EnvironmentRelease, EnvironmentReleaseId, RemoveOptions} from './environment-release';
import {EnvironmentReleaseService} from './environment-release.service';
import {ProgressMessages} from '../task/task';

@Component({
  selector: 'app-environment-releases',
  templateUrl: './environment-releases.component.html',
  styleUrls: ['./environment-releases.component.scss']
})
export class EnvironmentReleasesComponent implements OnInit {
  Role = Role;
  environmentReleases: EnvironmentRelease[] = undefined;
  environment: Environment;

  constructor(private route: ActivatedRoute,
              private environmentService: EnvironmentService,
              private environmentReleaseService: EnvironmentReleaseService,
              private releaseService: ReleaseService,
              private releaseManageService: ReleaseManageService,
              private log: LoggerService,
              private dialogService: DialogService,
              private confirmationService: ConfirmationService,
              private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const environmentId = +this.route.snapshot.paramMap.get('environmentId');

    this.environmentService.get(environmentId).pipe(
      mergeMap((env) => {
        this.environment = env;
        return this.loadReleasesForEnvironment();
      })
    ).subscribe(() => {
      this.setBreadcrumb();
    });
  }

  loadReleasesForEnvironment(): Observable<EnvironmentRelease[]> {
    return this.environmentReleaseService.getReleasesByEnvironment(this.environment.id).pipe(
      mergeMap((envReleases) => {
        this.environmentReleases = envReleases;
        return of(envReleases);
      })
    );
  }

  private setBreadcrumb() {
    this.breadcrumbService.setItems([
      {label: this.environment.key, routerLink: '/environments'},
      {label: 'releases'}
    ]);
  }

  addRelease() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Add a release to ${this.environment.name}:`;
    dialogConfig.data = this.environment;

    const dialogRef = this.dialogService.open(AddReleaseDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((release: Release) => {
        const environmentRelease = new EnvironmentRelease();
        environmentRelease.id = new EnvironmentReleaseId();

        environmentRelease.environment = this.environmentService.link(this.environment.id);
        environmentRelease.release = this.releaseService.link(release.id);

        return this.environmentReleaseService.save(environmentRelease);
      }),
      mergeMap(() => this.loadReleasesForEnvironment())
    ).subscribe();
  }

  removeRelease(event: Event, environmentRelease: EnvironmentRelease) {
    this.confirmationService.confirm({
      target: event.target,
      message: `Are you sure you want to remove ${(environmentRelease.release as Release).name} from ${(environmentRelease.environment as Environment).key}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        const relFromEnv = `${(environmentRelease.release as Release).name} from environment ${(environmentRelease.environment as Environment).key}`;
        const removeOptions = new RemoveOptions(
          new ProgressMessages(
            `Removing ${relFromEnv}`,
            `Removed ${relFromEnv}`,
            `Failed to remove ${relFromEnv}`
          ));
        this.releaseManageService.remove(environmentRelease, removeOptions).pipe(
          mergeMap(() => this.loadReleasesForEnvironment())
        ).subscribe();
      }
    });
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.environment.id === statusChangedEvent.environmentReleaseId.environmentId) {
        setTimeout(() => {
          this.loadReleasesForEnvironment().subscribe();
        }, 1000);
      }
    }
  }
}
