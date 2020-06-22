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

import {Component, OnInit} from '@angular/core';
import {ApplicationService} from './application.service';
import {Application} from './application';
import {ActivatedRoute} from '@angular/router';
import {LoggerService} from '../util/logger.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {filter} from 'rxjs/operators';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss']
})
export class ApplicationsComponent implements OnInit {
  applications: Application[] = undefined;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private log: LoggerService,
    private dialogService: DialogService) {
  }

  ngOnInit() {
    this.getApplications();
  }

  private getApplications() {
    this.log.debug('Getting all applications');
    this.applicationService.getAll()
      .subscribe(applications => this.applications = applications);
  }

  delete(application: Application) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${application.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = application.name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.applicationService.delete(application)
        .subscribe(() => this.getApplications());
    });
  }
}
