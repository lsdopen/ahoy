import {Injectable} from '@angular/core';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {ReleaseHistory} from './release-history';

@Injectable({
  providedIn: 'root'
})
export class ReleaseHistoryService {

  constructor(
    private log: LoggerService,
    private restClient: RestClientService) {
  }

  getAll(): Observable<ReleaseHistory[]> {
    const url = `/releaseHistories`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.releases as ReleaseHistory[]),
      tap((relHistories) => this.log.debug(`fetched ${relHistories.length} release history items`))
    );
  }

  getAllByReleaseId(releaseId: number): Observable<ReleaseHistory[]> {
    const url = `/releaseHistories/search/findByReleaseId?releaseId=${releaseId}&projection=releaseHistory`;
    return this.restClient.get<any>(url).pipe(
      map(response => response._embedded.releaseHistories as ReleaseHistory[]),
      tap((relHistories) => this.log.debug(`fetched ${relHistories.length} release history items`))
    );
  }

  get(id: number): Observable<ReleaseHistory> {
    const url = `/releaseHistories/${id}`;
    return this.restClient.get<ReleaseHistory>(url).pipe(
      tap((releaseHistory) => this.log.debug('fetched release history', releaseHistory))
    );
  }
}
