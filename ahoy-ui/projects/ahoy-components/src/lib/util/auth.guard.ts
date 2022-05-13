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
import {Observable} from 'rxjs';
import {Role} from './auth';
import {AuthService} from './auth.service';
import {LoggerService} from './logger.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private router: Router,
              private authService: AuthService,
              private log: LoggerService) {
  }

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const authenticated = this.authService.isAuthenticated();
    this.log.debug('user authenticated: ', authenticated);

    if (!authenticated) {
      this.log.warn('user not authenticated, starting login flow');
      this.authService.login();
      return false;
    }

    const roles = next.data.roles as Role[];
    if (roles && !this.authService.hasOneOfRole(roles)) {
      this.log.warn('user does not have one of the required roles: [' + roles + '] to navigate to: \'' + state.url + '\'');
      this.router.navigate(['/access']).then();
      return false;
    }
    this.log.debug('user has valid role');

    return true;
  }
}
