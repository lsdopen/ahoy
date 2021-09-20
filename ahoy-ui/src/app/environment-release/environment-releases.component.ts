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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ConfirmationService} from 'primeng/api';
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {filter, mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {Environment} from '../environments/environment';
import {EnvironmentService} from '../environments/environment.service';
import {ReleaseManageService} from '../release-manage/release-manage.service';
import {Release} from '../releases/release';
import {ReleaseService} from '../releases/release.service';
import {TaskEvent} from '../taskevents/task-events';
import {LoggerService} from '../util/logger.service';
import {AddReleaseDialogComponent} from './add-release-dialog/add-release-dialog.component';

@Component({
  selector: 'app-environment-releases',
  templateUrl: './environment-releases.component.html',
  styleUrls: ['./environment-releases.component.scss']
})
export class EnvironmentReleasesComponent implements OnInit {
  environments: Environment[] = undefined;
  environmentReleases: EnvironmentRelease[] = undefined;
  selectedEnvironment: Environment;

  constructor(private route: ActivatedRoute,
              private router: Router,
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
    this.setBreadcrumb();

    const environmentId = +this.route.snapshot.paramMap.get('environmentId');

    this.environmentService.getAll().subscribe((environments) => {
      this.environments = environments;
      this.getReleases(environmentId);
    });
  }

  private getReleases(environmentId) {
    this.log.debug('getting environment releases for environmentId=', environmentId);
    this.environmentService.get(environmentId)
      .subscribe(env => {
        this.selectedEnvironment = env;
        this.environmentReleaseService.getReleasesByEnvironment(environmentId)
          .subscribe(envReleases => {
            this.environmentReleases = envReleases;
            this.setBreadcrumb();
          });
      });
  }

  private setBreadcrumb() {
    if (this.selectedEnvironment) {
      this.breadcrumbService.setItems([
        {label: this.selectedEnvironment.name, routerLink: '/environments'},
        {label: 'releases'}
      ]);

    } else {
      this.breadcrumbService.setItems([{label: 'releases'}]);
    }
  }

  addRelease() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Add a release to ${this.selectedEnvironment.name}:`;
    dialogConfig.data = this.selectedEnvironment;

    const dialogRef = this.dialogService.open(AddReleaseDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((release: Release) => {
        const environmentRelease = new EnvironmentRelease();
        environmentRelease.id = new EnvironmentReleaseId();

        environmentRelease.environment = this.environmentService.link(this.selectedEnvironment.id);
        environmentRelease.release = this.releaseService.link(release.id);

        return this.environmentReleaseService.save(environmentRelease);
      })
    ).subscribe(() => {
      this.getReleases(this.selectedEnvironment.id);
    });
  }

  removeRelease(event: Event, environmentRelease: EnvironmentRelease) {
    this.confirmationService.confirm({
      target: event.target,
      message: `Are you sure you want to remove ${(environmentRelease.release as Release).name} from ${(environmentRelease.environment as Environment).name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.releaseManageService.remove(environmentRelease)
          .subscribe(() => this.getReleases(this.selectedEnvironment.id));
      }
    });
  }

  environmentChanged() {
    this.getReleases(this.selectedEnvironment.id);
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.selectedEnvironment.id === statusChangedEvent.environmentReleaseId.environmentId) {
        setTimeout(() => {
          this.getReleases(this.selectedEnvironment.id);
        }, 1000);
      }
    }
  }
}
