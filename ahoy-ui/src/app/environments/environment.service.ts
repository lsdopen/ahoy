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
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {EMPTY, Observable, of} from 'rxjs';
import {Environment} from './environment';
import {catchError, flatMap, map, tap} from 'rxjs/operators';
import {EnvironmentRelease} from '../environment-release/environment-release';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {
  private lastEnvironmentId: number;

  constructor(
    private log: LoggerService,
    private notificationsService: NotificationsService,
    private restClient: RestClientService) {
    this.lastEnvironmentId = 0;
  }

  getAll(): Observable<Environment[]> {
    return this.restClient.get<any>('/environments?projection=environment').pipe(
      map(response => response._embedded.environments as Environment[]),
      tap((envs) => this.log.debug(`fetched ${envs.length} environments`))
    );
  }

  getAllEnvironmentsByCluster(clusterId: number): Observable<Environment[]> {
    const url = `/clusters/${clusterId}/environments`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.environments as Environment[]),
      tap((envs) => this.log.debug(`fetched ${envs.length} environments for cluster=${clusterId}`))
    );
  }

  getAllForPromotion(environmentRelease: EnvironmentRelease): Observable<Environment[]> {
    const url = `/environments/search/forPromotion?releaseId=${environmentRelease.id.releaseId}`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.environments as Environment[]),
      tap((envs) => this.log.debug(`fetched ${envs.length} environments`))
    );
  }

  get(id: number): Observable<Environment> {
    const url = `/environments/${id}?projection=environment`;
    return this.restClient.get<Environment>(url).pipe(
      tap((env) => {
        this.lastEnvironmentId = env.id;
        this.log.debug('fetched environment', env);
      })
    );
  }

  create(environment: Environment): Observable<Environment> {
    this.log.debug('creating environment: ', environment);

    const url = `/environments/create`;

    return this.restClient.post<Environment>(url, environment, true).pipe(
      tap((createdEnvironment) => {
        this.log.debug('created environment', createdEnvironment);
        const text = `${createdEnvironment.name} ` + `was created in cluster ${createdEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to create environment ${environment.name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  destroy(environment: Environment): Observable<Environment> {
    this.log.debug('destroying environment: ', environment);

    const id = environment.id;
    const url = `/environments/destroy/${id}`;

    return this.restClient.delete<Environment>(url, true).pipe(
      tap((destroyedEnvironment) => {
        this.log.debug('destroyed environment', environment);
        const text = `${destroyedEnvironment.name} ` + `was destroyed from cluster ${destroyedEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to destroy environment ${environment.name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  duplicate(sourceEnvironment: Environment, destEnvironment: Environment): Observable<Environment> {
    this.log.debug('duplicating environment: ', sourceEnvironment);

    const url = `/environments/duplicate/${sourceEnvironment.id}/${destEnvironment.id}`;

    return this.restClient.post<Environment>(url, null, true).pipe(
      tap((duplicatedEnvironment) => {
        this.log.debug('duplicated environment', duplicatedEnvironment);
        const text = `${duplicatedEnvironment.name} ` + `was duplicated in cluster ${duplicatedEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to duplicate environment ${destEnvironment.name} from environment: ${sourceEnvironment.name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  getLastUsedId(): Observable<number> {
    if (this.lastEnvironmentId === 0) {
      this.log.debug('no last used environment found, finding first environment');
      return this.getAll().pipe(
        flatMap(environments => {
          this.lastEnvironmentId = environments.length > 0 ? environments[0].id : 0;
          return of(this.lastEnvironmentId);
        })
      );
    } else {
      return of(this.lastEnvironmentId);
    }
  }

  link(id: number): string {
    return this.restClient.getLink('/environments', id);
  }
}
