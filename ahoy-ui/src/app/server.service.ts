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
import {LoggerService} from './util/logger.service';
import {RestClientService} from './util/rest-client.service';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {ServerStatus} from './server';

@Injectable({
  providedIn: 'root'
})
export class ServerService {

  constructor(private restClient: RestClientService,
              private log: LoggerService) {
  }

  getServerStatus(): Observable<ServerStatus> {
    const url = `/api/server/status`;
    return this.restClient.get<ServerStatus>(url, false).pipe(
      tap((status) => {
        this.log.debug('fetched server status', status);
      })
    );
  }
}