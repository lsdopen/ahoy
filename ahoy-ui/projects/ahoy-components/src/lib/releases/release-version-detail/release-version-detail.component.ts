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
import {Release, ReleaseVersion} from '../release';
import {ReleaseService} from '../release.service';

@Component({
  selector: 'app-release-version-detail',
  templateUrl: './release-version-detail.component.html',
  styleUrls: ['./release-version-detail.component.scss']
})
export class ReleaseVersionDetailComponent implements OnInit {
  release: Release;
  releaseVersion: ReleaseVersion;
  editMode: boolean;
  editingVersion: string;

  constructor(private route: ActivatedRoute,
              private releaseService: ReleaseService,
              private location: Location,
              private breadcrumbService: AppBreadcrumbService) {
  }

  ngOnInit(): void {
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');
    const releaseVersionId = this.route.snapshot.paramMap.get('releaseVersionId');
    // TODO nested subscribes
    this.releaseService.getSummary(releaseId)
      .subscribe((release) => {
        this.release = release;

        if (releaseVersionId === 'new') {
          this.editMode = false;
          this.releaseVersion = new ReleaseVersion();

          this.setBreadcrumb();

        } else {
          this.editMode = true;
          this.releaseService.getVersion(+releaseVersionId)
            .subscribe((releaseVersion) => {
              this.releaseVersion = releaseVersion;
              this.editingVersion = releaseVersion.version;
              this.setBreadcrumb();
            });
        }
      });
  }

  private setBreadcrumb() {
    if (this.editMode) {
      this.breadcrumbService.setItems([
        {label: 'releases', routerLink: '/releases'},
        {label: this.release.name, routerLink: `/release/${this.release.id}}`},
        {label: this.releaseVersion.version},
        {label: 'edit'}
      ]);
    } else {
      this.breadcrumbService.setItems([
        {label: 'releases', routerLink: '/releases'},
        {label: this.release.name, routerLink: `/release/${this.release.id}}`},
        {label: 'new'}
      ]);
    }
  }

  save() {
    this.releaseVersion.release = this.releaseService.link(this.release.id);
    this.releaseService.saveVersion(this.releaseVersion)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.location.back();
  }
}
