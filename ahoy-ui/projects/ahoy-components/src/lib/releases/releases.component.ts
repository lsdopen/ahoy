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
import {ActivatedRoute, Router} from '@angular/router';
import {ConfirmationService} from 'primeng/api';
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {filter, mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {Environment} from '../environments/environment';
import {EnvironmentService} from '../environments/environment.service';
import {TaskEvent} from '../taskevents/task-events';
import {Role} from '../util/auth';
import {LoggerService} from '../util/logger.service';
import {AddToEnvironmentDialogComponent} from './add-to-environment-dialog/add-to-environment-dialog.component';
import {Release} from './release';
import {ReleaseService} from './release.service';

@Component({
  selector: 'app-releases',
  templateUrl: './releases.component.html',
  styleUrls: ['./releases.component.scss']
})
export class ReleasesComponent implements OnInit {
  Role = Role;
  releases: Release[] = undefined;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private releaseService: ReleaseService,
              private environmentService: EnvironmentService,
              private environmentReleaseService: EnvironmentReleaseService,
              private log: LoggerService,
              private dialogService: DialogService,
              private confirmationService: ConfirmationService,
              private breadcrumbService: AppBreadcrumbService) {

    this.breadcrumbService.setItems([{label: 'releases'}]);
  }

  ngOnInit() {
    this.getReleases();
  }

  private getReleases() {
    this.log.debug('getting all releases');
    this.releaseService.getAllSummary()
      .subscribe((releases) => {
        this.releases = releases;
      });
  }

  addToEnvironment(release: Release) {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Add ${release.name} to environment:`;
    dialogConfig.data = {release};

    // TODO nested subscribes
    const dialogRef = this.dialogService.open(AddToEnvironmentDialogComponent, dialogConfig);
    dialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((selectedEnvironment: Environment) => {
        const environmentRelease = new EnvironmentRelease();
        environmentRelease.id = new EnvironmentReleaseId();

        environmentRelease.environment = this.environmentService.link(selectedEnvironment.id);
        environmentRelease.release = this.releaseService.link(release.id);

        return this.environmentReleaseService.save(environmentRelease);
      })
    ).subscribe(() => this.getReleases());
  }

  deleteRelease(event, release: Release) {
    // TODO nested subscribes
    this.confirmationService.confirm({
      target: event.target,
      message: `Are you sure you want to delete ${release.name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.releaseService.delete(release)
          .subscribe(() => this.getReleases());
      }
    });
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      setTimeout(() => {
        this.getReleases();
      }, 1000);
    }
  }
}
