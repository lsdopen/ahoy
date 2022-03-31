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
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {EnvironmentReleaseId} from '../environment-release/environment-release';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {Application, ApplicationEnvironmentConfig, ApplicationEnvironmentConfigId, ApplicationEnvironmentConfigIdUtil, ApplicationEnvironmentSpec, ApplicationReleaseStatus, ApplicationVersion} from './application';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService) {
  }

  getAll(): Observable<Application[]> {
    const url = `/data/applications?projection=applicationSimple&sort=id`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applications as Application[]),
      tap(apps => this.log.debug(`fetched ${apps.length} apps`))
    );
  }

  getAllSummary(): Observable<Application[]> {
    const url = `/data/applications?projection=applicationSummary&sort=id`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applications as Application[]),
      tap(apps => this.log.debug(`fetched ${apps.length} apps`))
    );
  }

  getAllVersionsSummary(): Observable<ApplicationVersion[]> {
    const url = `/data/applicationVersions?projection=applicationVersionSummary`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationVersions as ApplicationVersion[]),
      tap(applicationVersions => this.log.debug(`fetched ${applicationVersions.length} application versions`))
    );
  }

  getAllVersionsForApplication(applicationId: number): Observable<ApplicationVersion[]> {
    const url = `/data/applications/${applicationId}/applicationVersions?projection=applicationVersionSimple`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationVersions as ApplicationVersion[]),
      tap(applicationVersions => this.log.debug(`fetched ${applicationVersions.length} application versions`))
    );
  }

  getAllVersionsSummaryForApplication(applicationId: number): Observable<ApplicationVersion[]> {
    const url = `/data/applications/${applicationId}/applicationVersions?projection=applicationVersionSummary`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationVersions as ApplicationVersion[]),
      tap(applicationVersions => this.log.debug(`fetched ${applicationVersions.length} application versions`))
    );
  }

  getAllVersionsForReleaseVersion(releaseVersionId: number): Observable<ApplicationVersion[]> {
    const url = `/data/releaseVersions/${releaseVersionId}/applicationVersions?projection=applicationVersionSummary`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationVersions as ApplicationVersion[]),
      tap(applicationVersions => this.log.debug(`fetched ${applicationVersions.length} application versions`))
    );
  }

  get(id: number): Observable<Application> {
    const url = `/data/applications/${id}?projection=applicationSummary`;
    return this.restClient.get<Application>(url).pipe(
      tap((app) => this.log.debug('fetched application', app))
    );
  }

  getVersion(id: number): Observable<ApplicationVersion> {
    const url = `/data/applicationVersions/${id}?projection=applicationVersionFull`;
    return this.restClient.get<ApplicationVersion>(url).pipe(
      tap((appVersion) => this.log.debug('fetched application version', appVersion))
    );
  }

  save(application: Application): Observable<Application> {
    this.log.debug('saving application', application);

    if (!application.id) {
      return this.restClient.post<Application>('/data/applications', application).pipe(
        tap((app) => this.log.debug('saved application', app))
      );

    } else {
      const url = `/data/applications/${application.id}`;
      return this.restClient.put(url, application).pipe(
        tap((app) => this.log.debug('updated application', app))
      );
    }
  }

  saveVersion(applicationVersion: ApplicationVersion): Observable<ApplicationVersion> {
    this.log.debug('saving application version', applicationVersion);

    if (!applicationVersion.id) {
      return this.restClient.post<ApplicationVersion>('/data/applicationVersions', applicationVersion).pipe(
        tap((appVersion) => this.log.debug('saved application version', appVersion))
      );

    } else {
      const url = `/data/applicationVersions/${applicationVersion.id}`;
      return this.restClient.put(url, applicationVersion).pipe(
        tap((appVersion) => this.log.debug('updated application version', appVersion))
      );
    }
  }

  delete(application: Application): Observable<Application> {
    const id = application.id;
    const url = `/data/applications/${id}`;

    return this.restClient.delete<Application>(url).pipe(
      tap(() => this.log.debug('deleted application', application))
    );
  }

  deleteVersion(applicationVersion: ApplicationVersion): Observable<ApplicationVersion> {
    const id = applicationVersion.id;
    const url = `/data/applicationVersions/${id}`;

    return this.restClient.delete<ApplicationVersion>(url).pipe(
      tap(() => this.log.debug('deleted application version', applicationVersion))
    );
  }

  saveEnvironmentConfig(environmentConfig: ApplicationEnvironmentConfig): Observable<ApplicationEnvironmentConfig> {
    this.log.debug('saving environment config', environmentConfig);

    if (!environmentConfig.id) {
      return this.restClient.post<ApplicationEnvironmentConfig>('/data/applicationEnvironmentConfigs', environmentConfig).pipe(
        tap((config) => this.log.debug('saved environment config', config))
      );

    } else {
      const url = `/data/applicationEnvironmentConfigs/${ApplicationEnvironmentConfigIdUtil.toIdString(environmentConfig)}`;
      return this.restClient.put<ApplicationEnvironmentConfig>(url, environmentConfig).pipe(
        tap((config) => this.log.debug('updated environment config', config))
      );
    }
  }

  getEnvironmentConfig(id: ApplicationEnvironmentConfigId): Observable<ApplicationEnvironmentConfig> {
    const url = `/data/applicationEnvironmentConfigs/${ApplicationEnvironmentConfigIdUtil.toIdStringFromId(id)}?projection=applicationEnvironmentConfigFull`;
    return this.restClient.get<ApplicationEnvironmentConfig>(url, false, () => {
      const defaultConfig = new ApplicationEnvironmentConfig();
      defaultConfig.id = id;
      defaultConfig.spec = new ApplicationEnvironmentSpec();
      return defaultConfig;
    }).pipe(
      tap((config) => this.log.debug('fetched application environment config', config))
    );
  }

  getExistingEnvironmentConfigs(environmentReleaseId: EnvironmentReleaseId,
                                releaseVersionId: number): Observable<ApplicationEnvironmentConfig[]> {
    const url =
      `/data/applicationEnvironmentConfigs/search/existingConfigs` +
      `?environmentId=${environmentReleaseId.environmentId}&releaseId=${environmentReleaseId.releaseId}` +
      `&releaseVersionId=${releaseVersionId}&projection=applicationEnvironmentConfigSummary`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationEnvironmentConfigs as ApplicationEnvironmentConfig[]),
      tap((existingConfigs) => this.log.debug('fetched existing environment configs', existingConfigs))
    );
  }

  getApplicationReleaseStatus(environmentReleaseId: EnvironmentReleaseId,
                              releaseVersionId: number): Observable<ApplicationReleaseStatus[]> {
    const url =
      `/data/applicationReleaseStatuses/search/byReleaseVersion` +
      `?environmentId=${environmentReleaseId.environmentId}&releaseId=${environmentReleaseId.releaseId}` +
      `&releaseVersionId=${releaseVersionId}`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.applicationReleaseStatuses as ApplicationReleaseStatus[]),
      tap((appReleaseStatuses) => this.log.debug('fetched application release statuses', appReleaseStatuses))
    );
  }

  link(id: number): string {
    return this.restClient.getLink('/data/applications', id);
  }
}
