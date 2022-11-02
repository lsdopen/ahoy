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
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {Cluster} from './cluster';

@Injectable({
  providedIn: 'root'
})
export class ClusterService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService,
    private notificationsService: NotificationsService) {
  }

  getAll(): Observable<Cluster[]> {
    const url = `/data/clusters?projection=clusterSimple&sort=id`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.clusters as Cluster[]),
      tap(apps => this.log.debug(`fetched ${apps.length} clusters`))
    );
  }

  get(id: number): Observable<Cluster> {
    const url = `/data/clusters/${id}?projection=clusterFull`;
    return this.restClient.get<Cluster>(url).pipe(
      tap((cluster) => {
        this.log.debug('fetched cluster', cluster);
      })
    );
  }

  save(cluster: Cluster): Observable<Cluster> {
    this.log.debug('saving cluster: ', cluster);

    if (!cluster.id) {
      return this.restClient.post<Cluster>('/data/clusters', cluster).pipe(
        tap((newCluster) => {
          this.log.debug('saved new cluster', newCluster);
        })
      );

    } else {
      const url = `/data/clusters/${cluster.id}`;
      return this.restClient.put(url, cluster).pipe(
        tap((updatedCluster) => {
          this.log.debug('updated cluster', updatedCluster);
        })
      );
    }
  }

  delete(cluster: Cluster): Observable<Cluster> {
    const id = cluster.id;
    const url = `/data/clusters/${id}`;

    return this.restClient.delete<Cluster>(url).pipe(
      tap(() => this.log.debug('deleted cluster', cluster))
    );
  }

  count(): Observable<number> {
    const url = `/data/clusters/count`;
    return this.restClient.get<number>(url).pipe(
      tap((count) => {
        this.log.debug('fetched cluster count', count);
      })
    );
  }

  deleteCascading(cluster: Cluster): Observable<Cluster> {
    this.log.debug('deleting cluster: ', cluster);

    const id = cluster.id;
    const url = `/data/clusters/delete/${id}`;

    return this.restClient.delete<Cluster>(url, true).pipe(
      tap((deletedCluster) => {
        this.log.debug('deleted cluster', cluster);
        const text = `${deletedCluster.name} ` + `was deleted`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to delete cluster ${cluster.name}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  link(id: number): string {
    return this.restClient.getLink('/data/clusters', id);
  }
}
