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
import {Router} from "@angular/router";
import {AuthConfig, NullValidationHandler, OAuthService} from "angular-oauth2-oidc";
import {LoggerService} from "./logger.service";
import {Observable} from "rxjs";
import {tap} from "rxjs/operators";
import {AuthInfo} from "./auth";
import {RestClientService} from "./rest-client.service";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authConfig: AuthConfig = {
    issuer: '',
    redirectUri: window.location.origin + '/#/dashboard?',
    postLogoutRedirectUri: window.location.origin + '/#/dashboard',
    clientId: 'ahoy',
    scope: 'openid profile email offline_access ahoy',
    responseType: 'code',
    // at_hash is not present in JWT token
    disableAtHashCheck: true,
    showDebugInformation: true
  };

  constructor(private router: Router,
              private oAuthService: OAuthService,
              private restClient: RestClientService,
              private log: LoggerService) {

    this.getAuthInfo().subscribe((authInfo) => {
      this.authConfig.issuer = authInfo.issuer;
      this.oAuthService.configure(this.authConfig);
      this.oAuthService.tokenValidationHandler = new NullValidationHandler();
      this.oAuthService.loadDiscoveryDocument().then(token => {
        this.oAuthService.tryLogin().then(_ => {
          this.router.navigate(['/']);
        })
      });
    });
  }

  public login() {
    this.oAuthService.initLoginFlow();
  }

  public logout() {
    this.log.debug('Logging out...');
    this.oAuthService.logOut();
  }

  public issuer(): string {
    return this.authConfig.issuer;
  }

  public accessToken(): string {
    return this.oAuthService.getAccessToken();
  }

  public identityClaim(): object {
    return this.oAuthService.getIdentityClaims();
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
}
