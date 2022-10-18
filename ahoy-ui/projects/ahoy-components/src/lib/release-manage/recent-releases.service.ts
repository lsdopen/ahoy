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

import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {EnvironmentRelease} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {Release} from '../releases/release';
import {LocalStorageService} from '../util/local-storage.service';

@Injectable({
  providedIn: 'root'
})
export class RecentReleasesService {
  private readonly KEY = 'recent-releases';
  private readonly RECENTS_TO_SHOW = 5;
  private recentReleases = new Map<number, RecentRelease>();
  private recentReleasesSubject = new Subject<RecentRelease[]>();

  constructor(private localStorageService: LocalStorageService,
              private environmentReleaseService: EnvironmentReleaseService) {

    const item = this.localStorageService.getItem(this.KEY);
    if (item) {
      const itemArr: RecentRelease[] = JSON.parse(item);
      for (const rr of itemArr) {
        this.recentReleases.set(rr.releaseId, rr);
      }
    }
  }

  public recent(environmentRelease: EnvironmentRelease, releaseVersionId: number) {
    const name = (environmentRelease.release as Release).name;
    this.recentReleases.set(environmentRelease.id.releaseId, new RecentRelease(name, environmentRelease, releaseVersionId));
    this.deleteOldest();
    this.recentReleasesUpdated();
  }

  public releaseUpdated(release: Release): void {
    const recentRelease = this.recentReleases.get(release.id);
    if (recentRelease) {
      recentRelease.name = release.name;
    }
    this.recentReleasesUpdated();
  }

  public recentReleasesChanged(): Observable<RecentRelease[]> {
    return this.recentReleasesSubject.asObservable();
  }

  public getRecentReleases(): RecentRelease[] {
    return Array.from(this.recentReleases.values())
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  public refresh() {
    this.environmentReleaseService.getAll()
      .subscribe((environmentReleases) => {

        let updated = false;
        for (const releaseId of this.recentReleases.keys()) {
          const recentRelease = this.recentReleases.get(releaseId);
          const releaseFound = environmentReleases.find((environmentRelease) => {
            const releaseVersions = (environmentRelease.release as Release).releaseVersions;
            return environmentRelease.id.environmentId === recentRelease.environmentId &&
              environmentRelease.id.releaseId === recentRelease.releaseId &&
              environmentRelease.id.environmentId === recentRelease.environmentId &&
              releaseVersions.find((releaseVersion) => releaseVersion.id === recentRelease.releaseVersionId);
          });

          if (!releaseFound) {
            updated = true;
            this.recentReleases.delete(releaseId);
          }
        }

        if (updated) {
          this.recentReleasesUpdated();
        }
      });
  }

  private recentReleasesUpdated() {
    const recentReleasesArr = this.getRecentReleases();
    this.localStorageService.setItem(this.KEY, JSON.stringify(recentReleasesArr));
    this.recentReleasesSubject.next(recentReleasesArr);
  }

  private deleteOldest() {
    if (this.recentReleases.size > this.RECENTS_TO_SHOW) {
      const sortedReleases = Array.from(this.recentReleases.values())
        .sort((a, b) => b.timestamp - a.timestamp);
      const poppedRecentRelease = sortedReleases.pop();
      if (poppedRecentRelease) {
        this.recentReleases.delete(poppedRecentRelease.releaseId);
      }
    }
  }
}

export class RecentRelease {
  name: string;
  environmentId: number;
  releaseId: number;
  releaseVersionId: number;
  timestamp: number;

  constructor(name: string, environmentRelease: EnvironmentRelease, releaseVersionId: number) {
    this.name = name;
    this.environmentId = environmentRelease.id.environmentId;
    this.releaseId = environmentRelease.id.releaseId;
    this.releaseVersionId = releaseVersionId;
    this.timestamp = new Date().getTime();
  }
}
