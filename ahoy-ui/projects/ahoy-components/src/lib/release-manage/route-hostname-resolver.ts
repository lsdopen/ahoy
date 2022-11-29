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

import {Application, ApplicationVersion} from '../applications/application';
import {Cluster} from '../clusters/cluster';
import {EnvironmentRelease} from '../environment-release/environment-release';
import {Environment} from '../environments/environment';
import {Release} from '../releases/release';

export class RouteHostnameResolver {

  public static resolve(environmentRelease: EnvironmentRelease, applicationVersion: ApplicationVersion, routeHostname: string): string {
    if (!routeHostname) {
      return routeHostname;
    }

    return routeHostname
      .replaceAll('${cluster_host}', ((environmentRelease.environment as Environment).cluster as Cluster).host)
      .replaceAll('${environment_key}', (environmentRelease.environment as Environment).key)
      .replaceAll('${release_name}', (environmentRelease.release as Release).name)
      .replaceAll('${application_name}', (applicationVersion.application as Application).name);
  }
}
