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
import {EMPTY, Observable} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
import {Cluster} from '../clusters/cluster';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {RecentReleasesService} from '../release-manage/recent-releases.service';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {DeleteOptions, DuplicateOptions, Environment, MoveOptions} from './environment';
import {ErrorUtil} from '../util/error-util';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {

  constructor(
    private log: LoggerService,
    private notificationsService: NotificationsService,
    private recentReleasesService: RecentReleasesService,
    private restClient: RestClientService) {
  }

  getAll(): Observable<Environment[]> {
    return this.restClient.get<any>('/data/environments?projection=environmentSummary&sort=orderIndex&sort=id').pipe(
      map(response => response._embedded.environments as Environment[]),
      tap((envs) => this.log.debug(`fetched ${envs.length} environments`))
    );
  }

  getAllForPromotion(releaseId: number): Observable<Environment[]> {
    const url = `/data/environments/search/forPromotion?releaseId=${releaseId}`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.environments as Environment[]),
      tap((envs) => this.log.debug(`fetched ${envs.length} environments`))
    );
  }

  get(id: number): Observable<Environment> {
    const url = `/data/environments/${id}?projection=environmentSimple`;
    return this.restClient.get<Environment>(url).pipe(
      tap((env) => {
        this.log.debug('fetched environment', env);
      })
    );
  }

  save(environment: Environment): Observable<Environment> {
    if (!environment.id) {
      this.log.debug('saving environment: ', environment);
      return this.restClient.post<Environment>('/data/environments', environment).pipe(
        tap((env) => this.log.debug('saved new environment', env))
      );

    } else {
      this.log.debug('updating environment: ', environment);
      const url = `/data/environments/${environment.id}`;
      return this.restClient.put(url, environment).pipe(
        tap((env) => this.log.debug('updated environment', env))
      );
    }
  }

  delete(environment: Environment, deleteOptions: DeleteOptions): Observable<Environment> {
    this.log.debug('deleting environment: ', environment);

    const id = environment.id;
    const url = `/api/environments/delete/${id}`;

    return this.restClient.delete<Environment>(url, deleteOptions).pipe(
      tap((deletedEnvironment) => {
        this.log.debug('deleted environment', deletedEnvironment);
        this.recentReleasesService.refresh();
      }),
      catchError((error) => {
        if (!ErrorUtil.is500Error(error)) {
          const text = `Failed to delete environment ${environment.key}`;
          this.notificationsService.notification(new Notification(text, error));
        }
        return EMPTY;
      })
    );
  }

  move(environment: Environment, moveOptions: MoveOptions): Observable<Environment> {
    this.log.debug(`moving environment: ${environment.key} to cluster: ${moveOptions.destClusterId}`);

    const url = `/api/environments/${environment.id}/move`;

    return this.restClient.post<Environment>(url, moveOptions, true).pipe(
      tap((destEnvironment) => {
        this.log.debug('moved environment', destEnvironment);
      }),
      catchError((error) => {
        if (!ErrorUtil.is500Error(error)) {
          const text = `Failed to move environment ${environment.key} to cluster`;
          this.notificationsService.notification(new Notification(text, error));
        }
        return EMPTY;
      })
    );
  }

  duplicate(sourceEnvironment: Environment, destEnvironment: Environment, duplicateOptions: DuplicateOptions): Observable<Environment> {
    this.log.debug('duplicating environment: ', sourceEnvironment);

    const url = `/api/environments/duplicate/${sourceEnvironment.id}/${destEnvironment.id}`;

    return this.restClient.post<Environment>(url, duplicateOptions, true).pipe(
      tap((duplicatedEnvironment) => {
        this.log.debug('duplicated environment', duplicatedEnvironment);
        const text = `${duplicatedEnvironment.key} ` + `was duplicated in cluster ${(duplicatedEnvironment.cluster as Cluster).name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to duplicate environment ${destEnvironment.key} from environment: ${sourceEnvironment.key}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  updateOrderIndex(environment: Environment): Observable<Environment> {
    this.log.debug('updating environment orderIndex: ', environment);

    const url = `/api/environments/${environment.id}/updateOrderIndex?orderIndex=${environment.orderIndex}`;

    return this.restClient.put<Environment>(url, null).pipe(
      tap(() => {
        this.log.debug('updated environment orderIndex', environment);
      }),
      catchError((error) => {
        const text = `Failed to update environment ${environment.key} order`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  link(id: number): string {
    return this.restClient.getLink('/data/environments', id);
  }
}
