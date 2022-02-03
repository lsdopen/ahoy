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
import {EMPTY, Observable, Subject} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {DeployOptions, EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {Environment} from '../environments/environment';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {PromoteOptions, Release, ReleaseVersion, UpgradeOptions} from '../releases/release';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {RecentReleasesService} from './recent-releases.service';
import {ArgoEvents, ResourceNode} from './resource';

@Injectable({
  providedIn: 'root'
})
export class ReleaseManageService {
  private environmentReleaseChangedSubject = new Subject<EnvironmentRelease>();

  constructor(private restClient: RestClientService,
              private notificationsService: NotificationsService,
              private recentReleasesService: RecentReleasesService,
              private log: LoggerService) {
  }

  deploy(environmentRelease: EnvironmentRelease, deployOptions: DeployOptions): Observable<EnvironmentRelease> {
    this.log.debug('deploying environment release', environmentRelease);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentRelease.id)}/deploy`;
    return this.restClient.post<EnvironmentRelease>(url, deployOptions, true).pipe(
      tap((deployedEnvironmentRelease) => {
        this.log.debug('deployed environment release', deployedEnvironmentRelease);
        this.environmentReleaseChangedSubject.next(deployedEnvironmentRelease);
        const text = `${(deployedEnvironmentRelease.release as Release).name} : ${deployedEnvironmentRelease.currentReleaseVersion.version} `
          + `deployed to environment ${(deployedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to deploy ${(environmentRelease.release as Release).name} : ${deployOptions.releaseVersionId} `
          + `to environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  undeploy(environmentRelease: EnvironmentRelease): Observable<EnvironmentRelease> {
    this.log.debug('undeploying environment release:', environmentRelease);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentRelease.id)}/undeploy`;
    return this.restClient.post<EnvironmentRelease>(url, null, true).pipe(
      tap((unDeployedEnvironmentRelease) => {
        this.log.debug('undeployed environment release', unDeployedEnvironmentRelease);
        this.environmentReleaseChangedSubject.next(unDeployedEnvironmentRelease);
        const text = `${(unDeployedEnvironmentRelease.release as Release).name} `
          + `was undeployed from environment ${(unDeployedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to undeploy ${(environmentRelease.release as Release).name} `
          + `from environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  remove(environmentRelease: EnvironmentRelease): Observable<EnvironmentRelease> {
    this.log.debug('removing environment release:', environmentRelease);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentRelease.id)}/remove`;
    return this.restClient.delete<EnvironmentRelease>(url, true).pipe(
      tap((removedEnvironmentRelease) => {
        this.log.debug('removed environment release', removedEnvironmentRelease);
        const text = `${(removedEnvironmentRelease.release as Release).name} `
          + `was removed from environment ${(removedEnvironmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
        this.recentReleasesService.refresh();
      }),
      catchError((error) => {
        const text = `Failed to remove ${(environmentRelease.release as Release).name} `
          + `from environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  promote(environmentReleaseId: EnvironmentReleaseId, promoteOptions: PromoteOptions): Observable<EnvironmentRelease> {
    this.log.debug(`promoting environment release: ${environmentReleaseId} to environment: ${promoteOptions.destEnvironmentId}`);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentReleaseId)}/promote`;
    return this.restClient.post<EnvironmentRelease>(url, promoteOptions).pipe(
      tap((environmentRelease) => {
        this.log.debug('promoted release to new environment', environmentRelease);
        const text = `${(environmentRelease.release as Release).name} `
          + `was promoted to environment ${(environmentRelease.environment as Environment).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to promote release`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  upgrade(releaseVersionId: number, upgradeOptions: UpgradeOptions): Observable<ReleaseVersion> {
    this.log.debug(`upgrading release version: ${releaseVersionId} to version: ${upgradeOptions.version}`);
    const url = `/api/releaseVersions/${releaseVersionId}/upgrade`;
    return this.restClient.post<ReleaseVersion>(url, upgradeOptions).pipe(
      tap((upgradedReleaseVersion) => this.log.debug('upgraded release version', upgradedReleaseVersion))
    );
  }

  copyEnvConfig(environmentReleaseId: EnvironmentReleaseId, sourceReleaseVersionId: number, destReleaseVersionId: number): Observable<EnvironmentRelease> {
    this.log.debug(`copying environment config for release: ${environmentReleaseId} from sourceReleaseVersionId: ${sourceReleaseVersionId} to destReleaseVersionId: ${destReleaseVersionId}`);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentReleaseId)}/copyEnvConfig?sourceReleaseVersionId=${sourceReleaseVersionId}&destReleaseVersionId=${destReleaseVersionId}`;
    return this.restClient.post<EnvironmentRelease>(url).pipe(
      tap((environmentRelease) => this.log.debug('copied environment config for release', environmentRelease))
    );
  }

  copyAppEnvConfig(releaseVersionId: number, sourceApplicationVersionId: number, destApplicationVersionId: number): Observable<EnvironmentRelease> {
    this.log.debug(`copying environment config for release version: ${releaseVersionId} from sourceApplicationVersionId: ${sourceApplicationVersionId} to destApplicationVersionId: ${destApplicationVersionId}`);
    const url = `/api/releaseVersions/${releaseVersionId}/copyAppEnvConfig?sourceApplicationVersionId=${sourceApplicationVersionId}&destApplicationVersionId=${destApplicationVersionId}`;
    return this.restClient.post<EnvironmentRelease>(url).pipe(
      tap(() => this.log.debug('copied environment config for release version', releaseVersionId))
    );
  }

  events(environmentReleaseId: EnvironmentReleaseId, resourceUid: string, resourceNamespace: string, resourceName: string): Observable<ArgoEvents> {
    this.log.debug(`getting events for environment release ${environmentReleaseId}, resourceUid: ${resourceUid}, resourceNamespace: ${resourceNamespace}, resourceName: ${resourceName}`);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentReleaseId)}/events?resourceUid=${resourceUid}&resourceNamespace=${resourceNamespace}&resourceName=${resourceName}`;
    return this.restClient.get<ArgoEvents>(url).pipe(
      tap((events) => this.log.debug('fetched events', events))
    );
  }

  resources(environmentReleaseId: EnvironmentReleaseId): Observable<ResourceNode> {
    this.log.debug('getting resources for environment release', environmentReleaseId);
    const url = `/api/environmentReleases/${EnvironmentReleaseId.pathValue(environmentReleaseId)}/resources`;
    return this.restClient.get<ResourceNode>(url).pipe(
      tap((resourceNode) => this.log.debug('fetched resources', resourceNode))
    );
  }

  public environmentReleaseChanged(): Observable<EnvironmentRelease> {
    return this.environmentReleaseChangedSubject.asObservable();
  }
}
