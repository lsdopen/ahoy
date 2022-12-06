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

import {Cluster} from '../clusters/cluster';
import {ProgressMessages} from '../task/task';

export class Environment {
  id: number;
  name: string;
  key: string;
  cluster: Cluster | string;
  orderIndex: number;
}

export class MoveOptions {
  destClusterId: number;
  redeployReleases = true;
  progressMessages: ProgressMessages;

  constructor(destClusterId: number, redeployReleases: boolean, progressMessages: ProgressMessages) {
    this.destClusterId = destClusterId;
    this.redeployReleases = redeployReleases;
    this.progressMessages = progressMessages;
  }
}

export class DuplicateOptions {
  copyEnvironmentConfig = false;

  constructor(copyEnvironmentConfig: boolean) {
    this.copyEnvironmentConfig = copyEnvironmentConfig;
  }
}

export class DeleteOptions {
  progressMessages: ProgressMessages;

  constructor(progressMessages: ProgressMessages) {
    this.progressMessages = progressMessages;
  }
}
