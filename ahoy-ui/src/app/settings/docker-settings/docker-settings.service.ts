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
