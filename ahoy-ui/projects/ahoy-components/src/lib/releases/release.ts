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

import {ApplicationVersion} from '../applications/application';
import {EnvironmentRelease} from '../environment-release/environment-release';

export class Release {
  id: number;
  name: string;
  releaseVersions: ReleaseVersion[];
  environmentReleases: EnvironmentRelease[];
  latestReleaseVersion: ReleaseVersion;
}

export class ReleaseVersion {
  id: number;
  version: string;
  release: Release | string;
  applicationVersions: ApplicationVersion[] = undefined;
}

export class PromoteOptions {
  destEnvironmentId: number;
  copyEnvironmentConfig = false;

  constructor();
  constructor(destEnvironmentId: number, copyEnvironmentConfig: boolean);
  constructor(destEnvironmentId?: number, copyEnvironmentConfig?: boolean) {
    this.destEnvironmentId = destEnvironmentId;
    this.copyEnvironmentConfig = copyEnvironmentConfig;
  }
}

export class UpgradeOptions {
  version: string;
  copyEnvironmentConfig = true;
}

export class UpgradeAppOptions {
  applicationVersion: ApplicationVersion;
  copyEnvironmentConfig = true;
}

export class DuplicateOptions {
  addToSameEnvironments = true;
  copyEnvironmentConfig = false;
}
