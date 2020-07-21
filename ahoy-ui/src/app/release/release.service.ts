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
import {EMPTY, Observable, Subject} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {RestClientService} from '../util/rest-client.service';
import {LoggerService} from '../util/logger.service';
import {DeployDetails, EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {Release, ReleaseVersion} from '../releases/release';
import {NotificationsService} from '../notifications/notifications.service';
import {Environment} from '../environments/environment';
import {Notification} from '../notifications/notification';

@Injectable({
  providedIn: 'root'
})
export class ReleaseService {
  private environmentReleaseChangedSubject = new Subject<EnvironmentRelease>();

  constructor(private restClient: RestClientService,
              private notificationsService: NotificationsService,
              private log: LoggerService) {
  }

  deploy(environmentRelease: EnvironmentRelease, releaseVersion: ReleaseVersion, deployDetails: DeployDetails): Observable<EnvironmentRelease> {
    this.log.debug('deploying environment release', environmentRelease);
    const url = `/api/release/deploy/${environmentRelease.id.environmentId}/${environmentRelease.id.releaseId}/${releaseVersion.id}`;
    return this.restClient.post<EnvironmentRelease>(url, deployDetails, true).pipe(
      tap((deployedEnvironmentRelease) => {
        this.log.debug('deployed environment release', deployedEnvironmentRelease);
        this.environmentReleaseChangedSubject.next(deployedEnvironmentRelease);
        const text = `${(deployedEnvironmentRelease.release as Release).name} : ${deployedEnvironmentRelease.currentReleaseVersion.version} `
          + `deployed to environment ${(deployedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to deploy ${(environmentRelease.release as Release).name} : ${releaseVersion.version} `
          + `to environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  undeploy(environmentRelease: EnvironmentRelease): Observable<EnvironmentRelease> {
    this.log.debug('undeploying environment release:', environmentRelease);
    const url = `/api/release/undeploy/${environmentRelease.id.environmentId}/${environmentRelease.id.releaseId}`;
    return this.restClient.post<EnvironmentRelease>(url, null, true).pipe(
      tap((unDeployedEnvironmentRelease) => {
        this.log.debug('undeployed environment release', unDeployedEnvironmentRelease);
        this.environmentReleaseChangedSubject.next(unDeployedEnvironmentRelease);
        const text = `${(unDeployedEnvironmentRelease.release as Release).name} `
          + `was undeployed from environment ${(unDeployedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to undeploy ${(environmentRelease.release as Release).name} `
          + `from environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  remove(environmentRelease: EnvironmentRelease): Observable<EnvironmentRelease> {
    this.log.debug('removing environment release:', environmentRelease);
    const url = `/api/release/remove/${environmentRelease.id.environmentId}/${environmentRelease.id.releaseId}`;
    return this.restClient.post<EnvironmentRelease>(url, null, true).pipe(
      tap((removedEnvironmentRelease) => {
        this.log.debug('removed environment release', removedEnvironmentRelease);
        const text = `${(removedEnvironmentRelease.release as Release).name} `
          + `was removed from environment ${(removedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to remove ${(environmentRelease.release as Release).name} `
          + `from environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  promote(environmentReleaseId: EnvironmentReleaseId, destEnvironmentId: number): Observable<EnvironmentRelease> {
    this.log.debug(`promoting environment release: ${environmentReleaseId} to environment: ${destEnvironmentId}`);
    const url = `/api/release/promote/${environmentReleaseId.environmentId}/${environmentReleaseId.releaseId}/${destEnvironmentId}`;
    return this.restClient.post<EnvironmentRelease>(url).pipe(
      tap((environmentRelease) => {
        this.log.debug('promoted release to new environment', environmentRelease);
        const text = `${(environmentRelease.release as Release).name} `
          + `was promoted to environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to promote release`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  upgrade(releaseVersionId: number, version: string): Observable<ReleaseVersion> {
    this.log.debug(`upgrading release version: ${releaseVersionId} to version: ${version}`);
    const url = `/api/release/upgrade/${releaseVersionId}/${version}`;
    return this.restClient.post<ReleaseVersion>(url).pipe(
      tap((upgradedReleaseVersion) => this.log.debug('upgraded release version', upgradedReleaseVersion))
    );
  }

  copyEnvConfig(environmentReleaseId: EnvironmentReleaseId, sourceReleaseVersionId: number, destReleaseVersionId: number): Observable<EnvironmentRelease> {
    this.log.debug(`copying environment config for release: ${environmentReleaseId} from sourceReleaseVersionId: ${sourceReleaseVersionId} to destReleaseVersionId: ${destReleaseVersionId}`);
    const url = `/api/release/copyEnvConfig/${environmentReleaseId.environmentId}/${environmentReleaseId.releaseId}/${sourceReleaseVersionId}/${destReleaseVersionId}`;
    return this.restClient.post<EnvironmentRelease>(url).pipe(
      tap((environmentRelease) => this.log.debug('copied environment config for release', environmentRelease))
    );
  }

  public environmentReleaseChanged(): Observable<EnvironmentRelease> {
    return this.environmentReleaseChangedSubject.asObservable();
  }
}
