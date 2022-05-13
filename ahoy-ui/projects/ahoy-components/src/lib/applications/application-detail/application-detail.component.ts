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
import {ActivatedRoute} from '@angular/router';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {ReleaseService} from '../../releases/release.service';
import {Application, ApplicationSpec, ApplicationVersion} from '../application';
import {ApplicationService} from '../application.service';

@Component({
  selector: 'app-application-detail',
  templateUrl: './application-detail.component.html',
  styleUrls: ['./application-detail.component.scss']
})
export class ApplicationDetailComponent implements OnInit {
  applicationVersion: ApplicationVersion;
  editMode = false;
  private releaseVersionId: number;
  applicationsForValidation: Application[];
  application: Application;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private releasesService: ReleaseService,
    private location: Location,
    private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id === 'new') {
      this.application = new Application();
      this.applicationVersion = new ApplicationVersion();
      this.applicationVersion.spec = new ApplicationSpec();
      this.setBreadcrumb();

    } else {
      this.editMode = true;
      this.applicationService.get(+id)
        .subscribe(app => {
          this.application = app;
          this.setBreadcrumb();
        });
    }

    this.releaseVersionId = +this.route.snapshot.queryParamMap.get('releaseVersionId');

    this.applicationService.getAll()
      .subscribe(applications => this.applicationsForValidation = applications);
  }

  private setBreadcrumb() {
    if (this.editMode) {
      this.breadcrumbService.setItems([
        {label: 'applications', routerLink: '/applications'},
        {label: this.application.name},
        {label: 'edit'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'applications', routerLink: '/applications'},
        {label: 'new'}
      ]);
    }
  }

  save() {
    // TODO nested subscribes
    if (!this.editMode) {
      this.applicationService.save(this.application)
        .subscribe((application) => {
          this.applicationVersion.application = this.applicationService.link(application.id);
          this.applicationService.saveVersion(this.applicationVersion)
            .subscribe((applicationVersion) => {
              if (this.releaseVersionId) {
                this.releasesService.associateApplication(this.releaseVersionId, applicationVersion.id)
                  .subscribe(() => this.location.back());
              } else {
                this.location.back();
              }
            });
        });
    } else {
      this.applicationService.save(this.application)
        .subscribe(() => this.location.back());
    }
  }

  cancel() {
    this.application = undefined;
    this.editMode = false;
    this.location.back();
  }
}
