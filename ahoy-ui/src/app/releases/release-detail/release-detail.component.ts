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
import {ReleasesService} from '../releases.service';
import {Release, ReleaseVersion} from '../release';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {Environment} from '../../environments/environment';
import {EnvironmentService} from '../../environments/environment.service';
import {mergeMap} from 'rxjs/operators';
import {LoggerService} from '../../util/logger.service';
import {ApplicationService} from '../../applications/application.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';

@Component({
  selector: 'app-release-detail',
  templateUrl: './release-detail.component.html',
  styleUrls: ['./release-detail.component.scss']
})
export class ReleaseDetailComponent implements OnInit {
  environment: Environment;
  editMode = false;
  release: Release;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  releasesForValidation: Release[];

  constructor(
    private log: LoggerService,
    private route: ActivatedRoute,
    private router: Router,
    private releasesService: ReleasesService,
    private environmentService: EnvironmentService,
    private environmentReleaseService: EnvironmentReleaseService,
    private applicationService: ApplicationService,
    private location: Location) {
  }

  ngOnInit() {
    const environmentId = +this.route.snapshot.paramMap.get('environmentId');
    const releaseId = this.route.snapshot.paramMap.get('releaseId');
    const releaseVersionId = +this.route.snapshot.paramMap.get('releaseVersionId');

    if (releaseId === 'new') {
      this.environmentService.get(environmentId)
        .subscribe(env => {
          this.environment = env;

          this.release = new Release();
          this.environmentRelease = new EnvironmentRelease();
          this.environmentRelease.id = new EnvironmentReleaseId();
          this.releaseVersion = new ReleaseVersion();
        });

    } else {
      this.editMode = true;
      this.getEnvironmentRelease(environmentId, +releaseId, releaseVersionId);
    }

    this.releasesService.getAll()
      .subscribe(rels => this.releasesForValidation = rels);
  }

  private getEnvironmentRelease(environmentId: number, releaseId: number, releaseVersionId: number) {
    this.environmentReleaseService.get(environmentId, releaseId)
      .subscribe(environmentRelease => {
        this.environmentRelease = environmentRelease as EnvironmentRelease;
        this.releaseVersion = (this.environmentRelease.release as Release).releaseVersions
          .find(relVersion => relVersion.id === releaseVersionId);
        this.release = this.environmentRelease.release as Release;
        this.environment = this.environmentRelease.environment as Environment;
      });
  }

  save() {
    if (!this.editMode) {
      this.releasesService.save(this.release)
        .pipe(
          mergeMap(release => {
            this.release = release;
            this.releaseVersion.release = this.releasesService.link(release.id);
            return this.releasesService.saveVersion(this.releaseVersion);
          }),
          mergeMap(releaseVersion => {
            this.environmentRelease.environment = this.environmentService.link(this.environment.id);
            this.environmentRelease.release = this.releasesService.link(this.release.id);

            return this.environmentReleaseService.save(this.environmentRelease);
          })
        )
        .subscribe(() => this.location.back());
    } else {
      this.releasesService.saveVersion(this.releaseVersion)
        .subscribe(() => {
          this.router.navigate(
            ['/release',
              this.environmentRelease.id.environmentId, this.environmentRelease.id.releaseId, 'version', this.releaseVersion.id]);
        });
    }
  }

  cancel() {
    this.environmentRelease = undefined;
    this.release = undefined;
    this.location.back();
  }
}
