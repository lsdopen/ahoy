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
import {Router} from '@angular/router';
import {AuthConfig, NullValidationHandler, OAuthService} from 'angular-oauth2-oidc';
import jwtDecode from 'jwt-decode';
import {EMPTY, Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {AuthInfo, Role} from './auth';
import {LoggerService} from './logger.service';
import {RestClientService} from './rest-client.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authConfig: AuthConfig = {
    issuer: '',
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    clientId: '',
    scope: 'openid profile email offline_access ahoy',
    responseType: 'code',
    // at_hash is not present in JWT token
    disableAtHashCheck: true,
    showDebugInformation: true
  };
  private authInfo: AuthInfo;
  private roles: Role[];

  constructor(private router: Router,
              private oAuthService: OAuthService,
              private restClient: RestClientService,
              private log: LoggerService) {

    this.getAuthInfo().subscribe((authInfo) => {
      this.authInfo = authInfo;
      this.authConfig.clientId = authInfo.clientId;
      this.authConfig.issuer = authInfo.issuer;
      this.oAuthService.configure(this.authConfig);
      this.oAuthService.setupAutomaticSilentRefresh();
      this.oAuthService.tokenValidationHandler = new NullValidationHandler();
      this.oAuthService.loadDiscoveryDocumentAndLogin().then(() => {
        this.loadRoles();
        this.router.initialNavigation();
      });
    }, (error) => {
      this.log.error('Error getting auth info', error);

      this.router.navigate(['/error']).then();
      return EMPTY;
    });
  }

  public login() {
    this.oAuthService.initLoginFlow();
  }

  public logout() {
    this.log.debug('Logging out...');
    this.authInfo = null;
    this.oAuthService.logOut();
  }

  public issuer(): string {
    return this.authConfig.issuer;
  }

  public accountUri(): string {
    return this.authInfo.accountUri;
  }

  public accessToken(): string {
    return this.oAuthService.getAccessToken();
  }

  public identityClaim(): object {
    return this.oAuthService.getIdentityClaims();
  }

  public userInitials(): string {
    // @ts-ignore
    const name = this.identityClaim().name;
    const names = name.split(' ');
    let initials = names[0].charAt(0).toUpperCase();

    if (names.length > 1) {
      initials += names[names.length - 1].charAt(0).toUpperCase();
    }
    return initials;
  }

  public isAuthenticated(): boolean {
    return this.oAuthService.hasValidIdToken() && this.oAuthService.hasValidAccessToken();
  }

  private getAuthInfo(): Observable<AuthInfo> {
    const url = `/auth/info`;
    return this.restClient.get<AuthInfo>(url, false, null).pipe(
      tap((authInfo) => this.log.debug('fetched auth info', authInfo))
    );
  }

  public hasRole(role: Role) {
    return this.roles.includes(role);
  }

  public hasOneOfRole(roles: Role[]) {
    return roles.some(r => this.hasRole(r));
  }

  private loadRoles() {
    try {
      this.log.debug('Loading roles...');
      this.roles = [];

      const rolesTokenPath = this.authInfo.rolesTokenPath;
      if (!rolesTokenPath) {
        this.log.error('Failed to load roles, roles token path not set');
        return;
      }

      let context = jwtDecode(this.oAuthService.getAccessToken()) as any;
      for (const pathContext of rolesTokenPath.split('.')) {
        context = context[pathContext];
        if (!context) {
          this.log.error('Failed to load roles, path context not found: ' + pathContext);
          return;
        }
      }

      for (const roleStr of context) {
        const role = Role[roleStr as keyof typeof Role];
        if (role) {
          this.roles.push(role);
        } else {
          this.log.warn('Role no found: ', roleStr);
        }
      }
      this.log.debug('Loaded roles: ', this.roles);

    } catch (error) {
      this.log.error('Failed to load roles', error);
    }
  }
}
