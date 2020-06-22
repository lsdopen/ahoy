import {Injectable} from '@angular/core';
import {LoggerService} from '../../util/logger.service';
import {RestClientService} from '../../util/rest-client.service';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {ArgoSettings} from './argo-settings';

@Injectable({
  providedIn: 'root'
})
export class ArgoSettingsService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService) {
  }

  get(): Observable<ArgoSettings> {
    const url = `/argoSettings/1`;
    return this.restClient.get<ArgoSettings>(url, false, () => {
      return new ArgoSettings(1);
    }).pipe(
      tap((settings) => {
        this.log.debug('fetched argo settings', settings);
      })
    );
  }

  exists(): Observable<boolean> {
    const url = `/argoSettings/1`;
    return this.restClient.exists(url, false).pipe(
      tap((exists) => {
        this.log.debug('checked argo settings exists: ', exists);
      })
    );
  }

  save(argoSettings: ArgoSettings): Observable<ArgoSettings> {
    this.log.debug('saving argo settings: ', argoSettings);

    return this.restClient.post<ArgoSettings>('/argoSettings', argoSettings, true).pipe(
      tap((savedSettings) => this.log.debug('saved argo settings', savedSettings))
    );
  }
}
