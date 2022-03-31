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

import {Component, Input} from '@angular/core';
import {ConfirmationService} from 'primeng/api';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {Role} from '../../util/auth';
import {Release, ReleaseVersion} from '../release';
import {ReleaseService} from '../release.service';

@Component({
  selector: 'app-release-versions',
  templateUrl: './release-versions.component.html',
  styleUrls: ['./release-versions.component.scss']
})
export class ReleaseVersionsComponent {
  Role = Role;
  @Input() release: Release;

  constructor(private releaseService: ReleaseService,
              private confirmationService: ConfirmationService) {
  }

  canDelete(releaseVersion: ReleaseVersion) {
    return !(this.isOnlyVersion(releaseVersion) || this.isDeployed(releaseVersion));
  }

  canDeleteTooltip(releaseVersion: ReleaseVersion): string {
    if (!this.canDelete(releaseVersion)) {
      if (this.isOnlyVersion(releaseVersion)) {
        return 'Release requires at least one version';
      }
      if (this.isDeployed(releaseVersion)) {
        return 'Unable to delete release version that is deployed to an environment';
      }
    }
    return '';
  }

  isOnlyVersion(releaseVersion: ReleaseVersion): boolean {
    return this.release.releaseVersions.length === 1 && this.release.releaseVersions[0] === releaseVersion;
  }

  isDeployed(releaseVersion: ReleaseVersion): boolean {
    return this.release.environmentReleases
      .filter((environmentRelease) => this.isDeployedInEnvironment(releaseVersion, environmentRelease))
      .length > 0;
  }

  isDeployedInEnvironment(releaseVersion: ReleaseVersion, environmentRelease: EnvironmentRelease): boolean {
    return environmentRelease.deployed && releaseVersion.id === environmentRelease.currentReleaseVersion.id;
  }

  delete(event: Event, releaseVersion: ReleaseVersion) {
    // TODO nested subscribes
    this.confirmationService.confirm({
      target: event.target,
      message: `Are you sure you want to delete version ${releaseVersion.version} from ${this.release.name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.releaseService.removeAssociatedApplications(releaseVersion.id)
          .subscribe(() => {
            this.releaseService.deleteVersion(releaseVersion)
              .subscribe(() => {
                const index = this.release.releaseVersions.indexOf(releaseVersion);
                if (index > -1) {
                  this.release.releaseVersions.splice(index, 1);
                }
              });
          });
      }
    });
  }
}
