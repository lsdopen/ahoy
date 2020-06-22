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

import {Release, ReleaseVersion} from '../releases/release';
import {Environment} from '../environments/environment';

export class EnvironmentRelease {
  id: EnvironmentReleaseId;
  environment: Environment | string;
  release: Release | string;
  deployed: boolean;
  status: string;
  currentReleaseVersion: ReleaseVersion;
  previousReleaseVersion: ReleaseVersion;
  latestReleaseVersion: ReleaseVersion;
  applicationsReady: number;
}

export class EnvironmentReleaseId {
  environmentId: number;
  releaseId: number;

  static new(environmentId: number, releaseId: number): EnvironmentReleaseId {
    const environmentReleaseId = new EnvironmentReleaseId();
    environmentReleaseId.environmentId = environmentId;
    environmentReleaseId.releaseId = releaseId;
    return environmentReleaseId;
  }
}

export class ReleaseStatusChangedEvent {
  environmentReleaseId: EnvironmentReleaseId;
  releaseVersionId: number;
}

export class DeployDetails {
  commitMessage: string;

  constructor(commitMessage: string) {
    this.commitMessage = commitMessage;
  }
}
