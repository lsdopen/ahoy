/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {Environment} from './environment';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {

  constructor(
    private log: LoggerService,
    private notificationsService: NotificationsService,
    private restClient: RestClientService) {
  }

  getAll(): Observable<Environment[]> {
    return this.restClient.get<any>('/data/environments?projection=environment&sort=orderIndex&sort=id').pipe(
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
    const url = `/data/environments/${id}?projection=environment`;
    return this.restClient.get<Environment>(url).pipe(
      tap((env) => {
        this.log.debug('fetched environment', env);
      })
    );
  }

  create(environment: Environment): Observable<Environment> {
    this.log.debug('creating environment: ', environment);

    const url = `/data/environments/create`;

    return this.restClient.post<Environment>(url, environment, true).pipe(
      tap((createdEnvironment) => {
        this.log.debug('created environment', createdEnvironment);
        const text = `${createdEnvironment.name} ` + `was created in cluster ${createdEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to create environment ${environment.name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  destroy(environment: Environment): Observable<Environment> {
    this.log.debug('destroying environment: ', environment);

    const id = environment.id;
    const url = `/data/environments/destroy/${id}`;

    return this.restClient.delete<Environment>(url, true).pipe(
      tap((destroyedEnvironment) => {
        this.log.debug('destroyed environment', environment);
        const text = `${destroyedEnvironment.name} ` + `was destroyed from cluster ${destroyedEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to destroy environment ${environment.name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  duplicate(sourceEnvironment: Environment, destEnvironment: Environment): Observable<Environment> {
    this.log.debug('duplicating environment: ', sourceEnvironment);

    const url = `/data/environments/duplicate/${sourceEnvironment.id}/${destEnvironment.id}`;

    return this.restClient.post<Environment>(url, null, true).pipe(
      tap((duplicatedEnvironment) => {
        this.log.debug('duplicated environment', duplicatedEnvironment);
        const text = `${duplicatedEnvironment.name} ` + `was duplicated in cluster ${duplicatedEnvironment.cluster.name}`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to duplicate environment ${destEnvironment.name} from environment: ${sourceEnvironment.name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  updateOrderIndex(environment: Environment): Observable<Environment> {
    this.log.debug('updating environment orderIndex: ', environment);

    const url = `/data/environments/${environment.id}/updateOrderIndex?orderIndex=${environment.orderIndex}`;

    return this.restClient.put<Environment>(url, null).pipe(
      tap(() => {
        this.log.debug('updated environment orderIndex', environment);
      }),
      catchError((error) => {
        const text = `Failed to update environment ${environment.name} order`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  link(id: number): string {
    return this.restClient.getLink('/data/environments', id);
  }
}
