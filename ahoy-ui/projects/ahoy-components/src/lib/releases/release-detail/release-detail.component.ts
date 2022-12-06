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
import {ActivatedRoute, Router} from '@angular/router';
import {of} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {ApplicationService} from '../../applications/application.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {Environment} from '../../environments/environment';
import {EnvironmentService} from '../../environments/environment.service';
import {DuplicateOptions, Release, ReleaseVersion} from '../release';
import {ReleaseService} from '../release.service';
import {TaskEvent} from '../../taskevents/task-events';
import {LoggerService} from '../../util/logger.service';

@Component({
  selector: 'app-release-detail',
  templateUrl: './release-detail.component.html',
  styleUrls: ['./release-detail.component.scss']
})
export class ReleaseDetailComponent implements OnInit {
  release: Release;
  releaseVersion: ReleaseVersion;
  releasesForValidation: Release[];
  editMode = false;
  environment: Environment;
  sourceRelease: Release;
  duplicateOptions: DuplicateOptions;

  constructor(
    private log: LoggerService,
    private route: ActivatedRoute,
    private router: Router,
    private releaseService: ReleaseService,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private applicationService: ApplicationService,
    private location: Location,
    private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const releaseId = this.route.snapshot.paramMap.get('releaseId');

    if (releaseId === 'new') {
      this.release = new Release();
      this.releaseVersion = new ReleaseVersion();

      const environmentId = +this.route.snapshot.queryParamMap.get('environmentId');
      if (environmentId) {
        this.environmentService.get(environmentId)
          .subscribe(env => {
            this.environment = env;
            this.setBreadcrumb();
          });
      } else {
        this.setBreadcrumb();
      }

      const sourceReleaseId = +this.route.snapshot.queryParamMap.get('sourceReleaseId');
      if (sourceReleaseId) {
        this.releaseService.get(sourceReleaseId)
          .subscribe((rel) => {
            this.duplicateOptions = new DuplicateOptions();
            this.sourceRelease = rel;
            this.setBreadcrumb();
          });
      }

    } else {
      this.editMode = true;
      this.getRelease(+releaseId);
    }

    this.releaseService.getAll()
      .subscribe(releases => this.releasesForValidation = releases);
  }

  private getRelease(releaseId: number) {
    this.releaseService.getSummary(releaseId)
      .subscribe(rel => {
        this.release = rel;
        this.setBreadcrumb();
      });
  }

  private setBreadcrumb() {
    if (this.editMode) {
      this.breadcrumbService.setItems([
        {label: 'releases', routerLink: '/releases'},
        {label: this.release.name},
        {label: 'edit'}
      ]);
    } else if (this.sourceRelease) {
      this.breadcrumbService.setItems([
        {label: 'releases', routerLink: '/releases'},
        {label: this.sourceRelease.name},
        {label: 'duplicate'}
      ]);
    } else if (this.environment) {
      this.breadcrumbService.setItems([
        {label: this.environment.key, routerLink: '/environments'},
        {label: 'new'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'releases', routerLink: '/releases'},
        {label: 'new'}
      ]);
    }
  }

  save() {
    this.releaseService.save(this.release)
      .pipe(
        mergeMap(release => {
          if (this.sourceRelease) {
            // we're duplicating a source release
            return this.releaseService.duplicate(this.sourceRelease, release, this.duplicateOptions);
          }
          return of(release);
        }),
        mergeMap(release => {
          this.release = release;
          if (!this.editMode && !this.sourceRelease) {
            this.releaseVersion.release = this.releaseService.link(release.id);
            return this.releaseService.saveVersion(this.releaseVersion);
          }
          return of(release);
        }),
        mergeMap(release => {
          if (this.environment) {
            const environmentRelease = new EnvironmentRelease();
            environmentRelease.id = new EnvironmentReleaseId();
            environmentRelease.environment = this.environmentService.link(this.environment.id);
            environmentRelease.release = this.releaseService.link(this.release.id);

            return this.environmentReleaseService.save(environmentRelease);
          }
          return of(release);
        })
      )
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.release = undefined;
    this.location.back();
  }

  isDeployed(): boolean {
    return this.release.environmentReleases &&
      this.release.environmentReleases.find((environmentRelease) => environmentRelease.deployed) !== undefined;
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.release.id === statusChangedEvent.environmentReleaseId.releaseId) {
        this.getRelease(this.release.id);
      }
    }
  }
}
