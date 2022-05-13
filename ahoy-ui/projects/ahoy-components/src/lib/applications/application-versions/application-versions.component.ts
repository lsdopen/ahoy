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

import {Component, Input, OnInit} from '@angular/core';
import {ConfirmationService} from 'primeng/api';
import {Release} from '../../releases/release';
import {Application, ApplicationVersion} from '../application';
import {ApplicationService} from '../application.service';

@Component({
  selector: 'app-application-versions',
  templateUrl: './application-versions.component.html',
  styleUrls: ['./application-versions.component.scss']
})
export class ApplicationVersionsComponent implements OnInit {
  @Input() application: Application;
  applicationVersions: ApplicationVersion[];

  constructor(private applicationService: ApplicationService,
              private confirmationService: ConfirmationService) {
  }

  ngOnInit(): void {
    this.loadApplicationVersions();
  }

  private loadApplicationVersions() {
    this.applicationService.getAllVersionsSummaryForApplication(this.application.id)
      .subscribe((applicationVersions) => this.applicationVersions = applicationVersions);
  }

  canDelete(applicationVersion: ApplicationVersion) {
    return !(applicationVersion.releaseVersions && applicationVersion.releaseVersions.length > 0);
  }

  usedByReleaseVersions(applicationVersion: ApplicationVersion): string[] {
    const usedBy = new Set<string>();
    for (const releaseVersion of applicationVersion.releaseVersions) {
      usedBy.add(`${(releaseVersion.release as Release).name}`);
    }
    return Array.from(usedBy.values()).sort();
  }

  delete(event: Event, applicationVersion: ApplicationVersion) {
    this.confirmationService.confirm({
      target: event.target,
      message: `Are you sure you want to delete version ${applicationVersion.version} from ${this.application.name}?`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.applicationService.deleteVersion(applicationVersion)
          .subscribe(() => this.loadApplicationVersions());
      }
    });
  }

  lastVersionId() {
    const length = this.applicationVersions.length;
    if (length > 0) {
      return this.applicationVersions[length - 1].id;
    }
    return -1;
  }
}
