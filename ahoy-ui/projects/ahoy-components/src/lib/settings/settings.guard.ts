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
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {Observable, of} from 'rxjs';
import {catchError, mergeMap} from 'rxjs/operators';
import {SettingsService} from './settings.service';
import {HttpErrorResponse} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class SettingsGuard implements CanActivate {
  private settingsConfigured = false;

  constructor(private settingsService: SettingsService,
              private router: Router) {
  }

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if (!this.settingsConfigured) {
      return this.settingsService.gitSettingsExists()
        .pipe(
          mergeMap((exists: boolean) => {
            if (!exists) {
              this.router.navigate(['/settings/git'], {queryParams: {setup: 'true'}}).then();
            } else {
              return this.settingsService.argoSettingsExists();
            }
            return of(exists);
          }),
          mergeMap((exists: boolean) => {
            this.settingsConfigured = exists;
            if (!exists) {
              this.router.navigate(['/settings/argo'], {queryParams: {setup: 'true'}}).then();
            }
            return of(exists);
          }),
          catchError((error) => {
            if (error instanceof HttpErrorResponse) {
              if (error.status === 401 || error.status === 403) {
                this.router.navigate(['/access']).then();
              }
            }
            throw error;
          })
        );
    }

    return true;
  }
}
