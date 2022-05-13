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
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {ReleaseHistory} from './release-history';

@Injectable({
  providedIn: 'root'
})
export class ReleaseHistoryService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService) {
  }

  getAllByReleaseId(releaseId: number): Observable<ReleaseHistory[]> {
    const url = `/data/releaseHistories/search/findByReleaseId?releaseId=${releaseId}&projection=releaseHistorySimple`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.releaseHistories as ReleaseHistory[]),
      tap((relHistories) => this.log.debug(`fetched ${relHistories.length} release history items`))
    );
  }

  get(id: number): Observable<ReleaseHistory> {
    const url = `/data/releaseHistories/${id}`;
    return this.restClient.get<ReleaseHistory>(url).pipe(
      tap((releaseHistory) => this.log.debug('fetched release history', releaseHistory))
    );
  }
}
