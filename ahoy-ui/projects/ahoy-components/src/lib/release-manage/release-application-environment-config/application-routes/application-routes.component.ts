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

import {AfterContentChecked, ChangeDetectorRef, Component, Input} from '@angular/core';
import {ApplicationRoute, ApplicationVersion} from '../../../applications/application';
import {ControlContainer, NgForm} from '@angular/forms';
import {RouteHostnameResolver} from '../../route-hostname-resolver';
import {EnvironmentRelease} from '../../../environment-release/environment-release';

@Component({
  selector: 'app-application-routes',
  templateUrl: './application-routes.component.html',
  styleUrls: ['./application-routes.component.scss'],
  viewProviders: [{provide: ControlContainer, useExisting: NgForm}]
})
export class ApplicationRoutesComponent implements AfterContentChecked {
  @Input() parentForm: NgForm;
  @Input() environmentRelease: EnvironmentRelease;
  @Input() applicationVersion: ApplicationVersion;
  @Input() routes: ApplicationRoute[];
  exampleRouteHost = '${release_name}-${application_name}-${environment_key}.${cluster_host}';

  constructor(private cd: ChangeDetectorRef) {
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  addRoute() {
    this.routes.push(new ApplicationRoute(this.exampleRouteHost, null));
  }

  removeRoute(route: ApplicationRoute) {
    const index = this.routes.indexOf(route);
    this.routes.splice(index, 1);
  }

  public resolveRouteHostname(route: ApplicationRoute): string {
    return RouteHostnameResolver.resolve(this.environmentRelease, this.applicationVersion, route.hostname);
  }
}
