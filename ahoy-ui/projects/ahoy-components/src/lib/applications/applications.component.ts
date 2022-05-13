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
import {filter} from 'rxjs/operators';
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {DialogUtilService} from '../components/dialog-util.service';
import {Release} from '../releases/release';
import {Role} from '../util/auth';
import {LoggerService} from '../util/logger.service';
import {Application} from './application';
import {ApplicationService} from './application.service';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss']
})
export class ApplicationsComponent implements OnInit {
  Role = Role;
  applications: Application[] = undefined;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private log: LoggerService,
    private dialogUtilService: DialogUtilService,
    private breadcrumbService: AppBreadcrumbService) {

    this.breadcrumbService.setItems([{label: 'applications'}]);
  }

  ngOnInit() {
    this.getApplications();
  }

  private getApplications() {
    this.log.debug('Getting all applications');
    this.applicationService.getAllSummary()
      .subscribe(applications => this.applications = applications);
  }

  canDelete(application: Application): boolean {
    for (const applicationVersion of application.applicationVersions) {
      if (applicationVersion.releaseVersions && applicationVersion.releaseVersions.length > 0) {
        return false;
      }
    }
    return true;
  }

  usedByReleases(application: Application): string[] {
    const usedBy = new Set<string>();
    for (const applicationVersion of application.applicationVersions) {
      for (const releaseVersion of applicationVersion.releaseVersions) {
        usedBy.add((releaseVersion.release as Release).name);
      }
    }
    return Array.from(usedBy.values()).sort();
  }

  delete(event: Event, application: Application) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${application.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = application.name;
    // TODO nested subscribes
    this.dialogUtilService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.applicationService.delete(application)
        .subscribe(() => this.getApplications());
    });
  }
}
