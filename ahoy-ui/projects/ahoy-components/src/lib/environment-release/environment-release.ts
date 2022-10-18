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

import {Environment} from '../environments/environment';
import {Release, ReleaseVersion} from '../releases/release';
import {ProgressMessages} from '../task/task';

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

  static pathValue(id: EnvironmentReleaseId): string {
    return `${id.environmentId}_${id.releaseId}`;
  }
}

export class ReleaseStatusChangedEvent {
  environmentReleaseId: EnvironmentReleaseId;
  releaseVersionId: number;
}

export class DeployOptions {
  releaseVersionId: number;
  commitMessage: string;
  progressMessages: ProgressMessages;

  constructor(releaseVersionId: number, commitMessage: string, progressMessages: ProgressMessages) {
    this.releaseVersionId = releaseVersionId;
    this.commitMessage = commitMessage;
    this.progressMessages = progressMessages;
  }
}

export class UndeployOptions {
  progressMessages: ProgressMessages;

  constructor(progressMessages: ProgressMessages) {
    this.progressMessages = progressMessages;
  }
}

export class RemoveOptions {
  progressMessages: ProgressMessages;

  constructor(progressMessages: ProgressMessages) {
    this.progressMessages = progressMessages;
  }
}
