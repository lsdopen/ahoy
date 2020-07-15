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

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authConfig: AuthConfig = {
    issuer: 'http://localhost:8081/auth/realms/Ahoy',
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
              private log: LoggerService) {

    this.oAuthService.configure(this.authConfig);
    this.oAuthService.tokenValidationHandler = new NullValidationHandler();
    this.oAuthService.loadDiscoveryDocument().then(token => {
      this.oAuthService.tryLogin().then(_ => {
        this.router.navigate(['/']);
      })
    });
  }

  public login() {
    this.oAuthService.initLoginFlow();
  }

  public logout() {
    this.log.debug('Logging out...');
    this.oAuthService.logOut();
  }

  public accessToken(): string {
    return this.oAuthService.getAccessToken();
  }

  public isAuthenticated(): boolean {
    return this.oAuthService.hasValidIdToken() && this.oAuthService.hasValidAccessToken();
  }
}
