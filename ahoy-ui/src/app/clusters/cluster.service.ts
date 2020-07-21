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
import {EMPTY, Observable, of} from 'rxjs';
import {catchError, flatMap, map, tap} from 'rxjs/operators';
import {RestClientService} from '../util/rest-client.service';
import {LoggerService} from '../util/logger.service';
import {Cluster} from './cluster';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';

@Injectable({
  providedIn: 'root'
})
export class ClusterService {
  private lastClusterId: number;

  constructor(
    private log: LoggerService,
    private restClient: RestClientService,
    private notificationsService: NotificationsService) {
    this.lastClusterId = 0;
  }

  getAll(): Observable<Cluster[]> {
    const url = `/data/clusters`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.clusters as Cluster[]),
      tap(apps => this.log.debug(`fetched ${apps.length} clusters`))
    );
  }

  get(id: number): Observable<Cluster> {
    const url = `/data/clusters/${id}`;
    return this.restClient.get<Cluster>(url).pipe(
      tap((cluster) => {
        if (cluster) {
          this.lastClusterId = cluster.id;
        }
        this.log.debug('fetched cluster', cluster);
      })
    );
  }

  save(cluster: Cluster): Observable<Cluster> {
    this.log.debug('saving cluster: ', cluster);

    if (!cluster.id) {
      return this.restClient.post<Cluster>('/data/clusters', cluster).pipe(
        tap((newCluster) => {
          this.lastClusterId = newCluster.id;
          this.log.debug('saved new cluster', newCluster);
        })
      );

    } else {
      const url = `/data/clusters/${cluster.id}`;
      return this.restClient.put(url, cluster).pipe(
        tap((updatedCluster) => {
          this.lastClusterId = updatedCluster.id;
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

  destroy(cluster: Cluster): Observable<Cluster> {
    this.log.debug('destroying cluster: ', cluster);

    const id = cluster.id;
    const url = `/data/clusters/destroy/${id}`;

    return this.restClient.delete<Cluster>(url, true).pipe(
      tap((destroyedCluster) => {
        this.log.debug('destroyed cluster', cluster);
        const text = `${destroyedCluster.name} ` + `was destroyed`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError(() => {
        const text = `Failed to destroy cluster ${cluster.name}`;
        this.notificationsService.notification(new Notification(text, true));
        return EMPTY;
      })
    );
  }

  getLastUsedId(): Observable<number> {
    if (this.lastClusterId === 0) {
      this.log.debug('no last used cluster found, finding first cluster...');
      return this.getAll().pipe(
        flatMap((clusters) => {
          this.lastClusterId = clusters.length > 0 ? clusters[0].id : 0;
          return of(this.lastClusterId);
        })
      );
    } else {
      return of(this.lastClusterId);
    }
  }

  link(id: number): string {
    return this.restClient.getLink('/data/clusters', id);
  }
}
