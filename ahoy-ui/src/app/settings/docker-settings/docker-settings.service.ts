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
import {LoggerService} from '../../util/logger.service';
import {RestClientService} from '../../util/rest-client.service';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {DockerSettings} from './docker-settings';

@Injectable({
  providedIn: 'root'
})
export class DockerSettingsService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService) {
  }

  get(): Observable<DockerSettings> {
    const url = `/dockerSettings/1`;
    return this.restClient.get<DockerSettings>(url, false, () => {
      return new DockerSettings(1);
    }).pipe(
      tap((settings) => {
        this.log.debug('fetched docker settings', settings);
      })
    );
  }

  exists(): Observable<boolean> {
    const url = `/dockerSettings/1`;
    return this.restClient.exists(url, false).pipe(
      tap((exists) => {
        this.log.debug('checked docker settings exists: ', exists);
      })
    );
  }

  save(dockerSettings: DockerSettings): Observable<DockerSettings> {
    this.log.debug('saving docker settings: ', dockerSettings);

    return this.restClient.post<DockerSettings>('/dockerSettings', dockerSettings, true).pipe(
      tap((savedSettings) => this.log.debug('saved docker settings', savedSettings))
    );
  }
}
