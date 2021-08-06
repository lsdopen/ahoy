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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EnvironmentService} from '../environments/environment.service';
import {Environment} from '../environments/environment';
import {LoggerService} from '../util/logger.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {filter, mergeMap} from 'rxjs/operators';
import {AddReleaseDialogComponent} from './add-release-dialog/add-release-dialog.component';
import {ReleasesService} from './releases.service';
import {Release} from './release';
import {TaskEvent} from '../taskevents/task-events';
import {ReleaseService} from '../release/release.service';
import {ConfirmationService, MessageService} from 'primeng/api';
import {DialogService, DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';

@Component({
  selector: 'app-releases',
  templateUrl: './releases.component.html',
  styleUrls: ['./releases.component.scss'],
  styles: [`
    :host ::ng-deep .p-dialog .product-image {
      width: 150px;
      margin: 0 auto 2rem auto;
      display: block;
    }

    @media screen and (max-width: 960px) {
      :host ::ng-deep .p-datatable.p-datatable-releases .p-datatable-tbody > tr > td:last-child {
        text-align: center;
      }

      :host ::ng-deep .p-datatable.p-datatable-releases .p-datatable-tbody > tr > td:nth-child(6) {
        display: flex;
      }
    }

  `],
  providers: [MessageService, ConfirmationService, DialogService]
})
export class ReleasesComponent implements OnInit, OnDestroy {
  environments: Environment[] = undefined;
  environmentReleases: EnvironmentRelease[] = undefined;
  selectedEnvironment: Environment;
  selectedEnvironmentRelease: EnvironmentRelease;

  cols: any[];
  addReleaseDialogRef: DynamicDialogRef;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private environmentService: EnvironmentService,
              private environmentReleaseService: EnvironmentReleaseService,
              private releasesService: ReleasesService,
              private releaseService: ReleaseService,
              private log: LoggerService,
              private dialogService: DialogService,
              private confirmationService: ConfirmationService,
              private messageService: MessageService) {
  }

  ngOnInit() {
    this.cols = [
      {field: 'release', header: 'Release'},
      {field: 'state', header: 'State'},
      {field: 'applications', header: 'Applications'},
      {field: 'version', header: 'Version'}
    ];

    const environmentId = +this.route.snapshot.queryParamMap.get('environmentId');

    this.environmentService.getAll().subscribe((environments) => {
      this.environments = environments;

      if (environmentId === 0) {
        this.environmentService.getLastUsedId().subscribe((lastUsedEnvironmentId) => {
          if (lastUsedEnvironmentId !== 0) {
            this.getReleases(lastUsedEnvironmentId);
          }
        });
      } else {
        this.getReleases(environmentId);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.addReleaseDialogRef) {
      this.addReleaseDialogRef.close();
    }
  }

  private getReleases(environmentId) {
    this.log.debug('getting environment releases for environmentId=', environmentId);
    this.environmentService.get(environmentId)
      .subscribe(env => {
        this.selectedEnvironment = env;
        this.environmentReleaseService.getReleasesByEnvironment(environmentId)
          .subscribe(envReleases => this.environmentReleases = envReleases);
      });
  }

  addRelease() {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = `Add release to ${this.selectedEnvironment.cluster.name}/${this.selectedEnvironment.name}`;
    dialogConfig.data = this.selectedEnvironment;

    this.addReleaseDialogRef = this.dialogService.open(AddReleaseDialogComponent, dialogConfig);
    this.addReleaseDialogRef.onClose.pipe(
      filter((result) => result !== undefined), // cancelled
      mergeMap((release: Release) => {
        const environmentRelease = new EnvironmentRelease();
        environmentRelease.id = new EnvironmentReleaseId();

        environmentRelease.environment = this.environmentService.link(this.selectedEnvironment.id);
        environmentRelease.release = this.releasesService.link(release.id);

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
        this.releaseService.remove(environmentRelease)
          .subscribe(() => {
            this.messageService.add({
              severity: 'info',
              summary: 'Removed',
              detail: `Removed ${(environmentRelease.release as Release).name} from ${(environmentRelease.environment as Environment).name}`
            });
            this.getReleases(this.selectedEnvironment.id);
          });
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
