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

import {Component, Inject, OnInit} from '@angular/core';
import {ApplicationService} from '../../applications/application.service';
import {Application, ApplicationVersion} from '../../applications/application';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {ReleaseVersion} from '../release';

@Component({
  selector: 'app-add-application',
  templateUrl: './add-application-dialog.component.html',
  styleUrls: ['./add-application-dialog.component.scss']
})
export class AddApplicationDialogComponent implements OnInit {
  applications: Application[];
  selectedApplication: Application;
  currentApplicationVersion: ApplicationVersion;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  applicationVersions: ApplicationVersion[];
  selectedVersion: ApplicationVersion;
  applicationMode = false;
  linkedApplications: Application[];

  constructor(
    private applicationService: ApplicationService,
    @Inject(MAT_DIALOG_DATA) data) {
    this.environmentRelease = data.environmentRelease;
    this.releaseVersion = data.releaseVersion;
    this.linkedApplications = this.releaseVersion.applicationVersions.map(appVersion => appVersion.application as Application);

    // upgrade?
    if (data.currentApplicationVersion) {
      this.applicationMode = true;
      this.currentApplicationVersion = data.currentApplicationVersion;
      this.selectedApplication = this.currentApplicationVersion.application as Application;
      this.applicationChanged();
    }
  }

  ngOnInit() {
    this.applicationService.getAll()
      .subscribe(applications => this.applications = applications);
  }

  applicationChanged() {
    if (this.selectedApplication) {
      this.applicationService.getAllVersionsForApplication(this.selectedApplication.id)
        .subscribe((applicationVersions) => this.applicationVersions = applicationVersions);
    }
  }
}
