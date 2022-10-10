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

import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Injectable, isDevMode} from '@angular/core';
import {Observable, of, throwError} from 'rxjs';
import {catchError, mergeMap, tap} from 'rxjs/operators';
import {ProgressService} from '../progress.service';
import {LoggerService} from './logger.service';

@Injectable({
  providedIn: 'root'
})
export class RestClientService {
  private appsUrl = '';
  private defaultOptions = {};
  private defaultHeaders = {'Content-Type': 'application/json'};

  constructor(
    private log: LoggerService,
    private progressService: ProgressService,
    private http: HttpClient) {
    if (isDevMode()) {
      this.appsUrl = 'http://localhost:8080' + this.appsUrl;
    }
  }

  getLink(path: string, id: number) {
    return this.appsUrl + path + '/' + id;
  }

  post<T>(path: string, body?: T | any, progress = false): Observable<T> {
    this.log.trace('posting to path: ' + path);
    this.startProgress(progress);
    return this.http.post<T>(this.appsUrl + path, body, this.createOptions()).pipe(
      tap(() => this.stopProgress(progress)),
      catchError(this.handleError<T>(progress))
    );
  }

  put<T>(path: string, body?: T, headers?: { [key: string]: string }, progress = false): Observable<T> {
    this.log.trace('putting to path: ' + path);
    this.startProgress(progress);
    return this.http.put<T>(this.appsUrl + path, body, this.createOptions(undefined, headers)).pipe(
      tap(() => this.stopProgress(progress)),
      catchError(this.handleError<T>(progress))
    );
  }

  patch<T>(path: string, body?: T, headers?: { [key: string]: string }, progress = false): Observable<T> {
    this.log.trace('patching to path: ' + path);
    this.startProgress(progress);
    return this.http.patch<T>(this.appsUrl + path, body, this.createOptions(undefined, headers)).pipe(
      tap(() => this.stopProgress(progress)),
      catchError(this.handleError<T>(progress))
    );
  }

  get<T>(path: string, progress = false, defaultIfNotFound?: () => T, options?: { [key: string]: string }, headers?: { [key: string]: string }): Observable<T> {
    this.log.trace('getting from path: ' + path);
    this.startProgress(progress);
    return this.http.get<T>(this.appsUrl + path, this.createOptions(options, headers)).pipe(
      tap(() => this.stopProgress(progress)),
      catchError(this.handleError<T>(progress, defaultIfNotFound))
    );
  }

  exists(path: string, progress = false): Observable<boolean> {
    this.log.trace('checking existence at path: ' + path);
    this.startProgress(progress);
    return this.http.get(this.appsUrl + path, this.createOptions()).pipe(
      mergeMap(() => {
          this.stopProgress(progress);
          return of(true);
        }
      ),
      catchError(this.handleExists(progress))
    );
  }

  delete<T>(path: string, progress?: boolean): Observable<T>;
  delete<T>(path: string, body?: T | any, progress?: boolean): Observable<T>;
  delete<T>(path: string, body?: T | any, progress?: boolean): Observable<T> {
    this.log.trace('deleting at path: ' + path);
    this.startProgress(progress);
    return this.http.delete<T>(this.appsUrl + path, this.createOptions({body})).pipe(
      tap(() => this.stopProgress(progress)),
      catchError(this.handleError<T>(progress))
    );
  }

  private startProgress(progress: boolean) {
    if (progress) {
      this.progressService.showProgress(true);
    }
  }

  private stopProgress(progress: boolean) {
    if (progress) {
      this.progressService.showProgress(false);
    }
  }

  private handleError<T>(progress: boolean, defaultIfNotFound?: () => T) {
    return (error: any): Observable<T> => {

      this.stopProgress(progress);

      if (defaultIfNotFound && error instanceof HttpErrorResponse && error.status === 404) {
        return of(defaultIfNotFound());

      } else {
        this.log.error('Rest client error', error);
        return throwError(error);
      }
    };
  }

  private handleExists(progress: boolean) {
    return (error: any): Observable<boolean> => {

      this.stopProgress(progress);

      if (error instanceof HttpErrorResponse && error.status === 404) {
        return of(false);

      } else {
        this.log.error('Rest client error', error);
        return throwError(error);
      }
    };
  }

  private createOptions(options?: { [key: string]: string },
                        headers?: { [key: string]: string }) {

    const allHeaders = {...this.defaultHeaders, ...headers};
    return {
      headers: new HttpHeaders(allHeaders),
      ...this.defaultOptions,
      ...options
    };
  }
}
