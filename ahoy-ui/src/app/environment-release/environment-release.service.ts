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

import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {RestClientService} from '../util/rest-client.service';
import {LoggerService} from '../util/logger.service';
import {EnvironmentRelease, EnvironmentReleaseId} from './environment-release';
import {NotificationsService} from '../notifications/notifications.service';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentReleaseService {

  public static environmentReleaseEquals(er1: EnvironmentRelease, er2: EnvironmentRelease): boolean {
    return this.environmentReleaseIdEquals(er1.id, er2.id);
  }

  public static environmentReleaseIdEquals(er1: EnvironmentReleaseId, er2: EnvironmentReleaseId): boolean {
    return er1.environmentId === er2.environmentId && er1.releaseId === er2.releaseId;
  }

  constructor(private restClient: RestClientService,
              private notificationsService: NotificationsService,
              private log: LoggerService) {
  }

  getReleasesByEnvironment(environmentId: number): Observable<EnvironmentRelease[]> {
    const url = `/data/environments/${environmentId}/environmentReleases?projection=environmentRelease`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.environmentReleases as EnvironmentRelease[]),
      tap((envReleases) => this.log.debug(`fetched ${envReleases.length} environment releases for environment=${environmentId}`))
    );
  }

  getReleasesByRelease(releaseId: number): Observable<EnvironmentRelease[]> {
    const url = `/data/environmentReleases/search/byRelease?releaseId=${releaseId}&projection=environmentRelease`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.environmentReleases as EnvironmentRelease[]),
      tap((envReleases) => this.log.debug(`fetched ${envReleases.length} environment releases for release=${releaseId}`))
    );
  }

  get(environmentId: number, releaseId: number): Observable<EnvironmentRelease> {
    const url = `/data/environmentReleases/${environmentId}_${releaseId}?projection=environmentRelease`;
    return this.restClient.get<EnvironmentRelease>(url).pipe(
      tap((envRelease) => this.log.debug('fetched environment release', envRelease))
    );
  }

  save(environmentRelease: EnvironmentRelease): Observable<EnvironmentRelease> {
    this.log.debug('saving environment release', environmentRelease);
    return this.restClient.post<EnvironmentRelease>('/data/environmentReleases', environmentRelease).pipe(
      tap((envRelease) => this.log.debug('saved new environment release in environment', envRelease))
    );
  }
}
