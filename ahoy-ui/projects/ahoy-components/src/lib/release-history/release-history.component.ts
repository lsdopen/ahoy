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
import {AppBreadcrumbService} from '../app.breadcrumb.service';
import {Description} from '../components/description-dialog/description';
import {DialogUtilService} from '../components/dialog-util.service';
import {Release} from '../releases/release';
import {ReleaseService} from '../releases/release.service';
import {ReleaseHistory} from './release-history';
import {ReleaseHistoryService} from './release-history.service';

@Component({
  selector: 'app-release-history',
  templateUrl: './release-history.component.html',
  styleUrls: ['./release-history.component.scss']
})
export class ReleaseHistoryComponent implements OnInit {
  releaseHistories: ReleaseHistory[];
  release: Release;

  constructor(private route: ActivatedRoute,
              private location: Location,
              private releasesService: ReleaseService,
              private releaseHistoryService: ReleaseHistoryService,
              private dialogUtilService: DialogUtilService,
              private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit() {
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');
    // TODO nested subscribes
    this.releasesService.get(releaseId)
      .subscribe((release) => {
        this.release = release;
        this.setBreadcrumb();

        this.releaseHistoryService.getAllByReleaseId(releaseId)
          .subscribe((releaseHistories) => this.releaseHistories = releaseHistories);
      });
  }

  private setBreadcrumb() {
    this.breadcrumbService.setItems([
      {label: this.release.name},
      {label: 'history'}
    ]);
  }

  done() {
    this.location.back();
  }

  showDescription(event: Event, releaseHistory: ReleaseHistory) {
    this.dialogUtilService.showDescriptionDialog(new Description('Description', releaseHistory.description));
  }
}
