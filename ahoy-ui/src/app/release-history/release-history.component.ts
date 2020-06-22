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
import {ReleaseHistory} from './release-history';
import {ActivatedRoute} from '@angular/router';
import {ReleaseHistoryService} from './release-history.service';
import {ReleasesService} from '../releases/releases.service';
import {Release} from '../releases/release';
import {Location} from '@angular/common';
import {Description} from '../components/description-dialog/description';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-release-history',
  templateUrl: './release-history.component.html',
  styleUrls: ['./release-history.component.scss']
})
export class ReleaseHistoryComponent implements OnInit {
  private displayedColumns = ['version', 'action', 'status', 'environment', 'cluster', 'date', 'description'];
  private releaseHistories: ReleaseHistory[];
  release: Release;

  constructor(private route: ActivatedRoute,
              private location: Location,
              private releasesService: ReleasesService,
              private releaseHistoryService: ReleaseHistoryService,
              private dialogService: DialogService) {
  }

  ngOnInit() {
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');

    this.releasesService.get(releaseId)
      .subscribe((release) => {
        this.release = release;
        this.releaseHistoryService.getAllByReleaseId(releaseId)
          .subscribe((releaseHistories) => this.releaseHistories = releaseHistories);
      });
  }

  done() {
    this.location.back();
  }

  showDescription(releaseHistory: ReleaseHistory) {
    this.dialogService.showDescriptionDialog(new Description('Description', releaseHistory.description));
  }
}
