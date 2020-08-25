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

import {EnvironmentReleaseId} from '../environment-release/environment-release';
import {DockerRegistry} from '../settings/docker-settings/docker-settings';

export class Application {
  id: number;
  name: string;
  applicationVersions: ApplicationVersion[];
  latestApplicationVersion: ApplicationVersion;
}

export class ApplicationVersion {
  id: number;
  dockerRegistry: DockerRegistry;
  image: string;
  version: string;
  environmentVariables: ApplicationEnvironmentVariable[];
  healthEndpointPath: string;
  healthEndpointPort: number;
  healthEndpointScheme: string;
  servicePorts: number[];
  application: Application | string;
  configs: ApplicationConfig[];
  volumes: ApplicationVolume[];
  secrets: ApplicationSecret[];
  configPath: string;
  environmentConfig: ApplicationEnvironmentConfig;
  status: ApplicationReleaseStatus;
}

export class ApplicationEnvironmentVariable {
  id: number;
  key: string;
  value: string;
  type: string;
  secretName: string;
  secretKey: string;

  static newValueType(key: string, value: string): ApplicationEnvironmentVariable {
    let environmentVariable = new ApplicationEnvironmentVariable();
    environmentVariable.type = 'Value';
    environmentVariable.key = key;
    environmentVariable.value = value;
    return environmentVariable;
  }

  static newSecretType(key: string, secretName: string, secretKey: string): ApplicationEnvironmentVariable {
    let environmentVariable = new ApplicationEnvironmentVariable();
    environmentVariable.type = 'Secret';
    environmentVariable.key = key;
    environmentVariable.secretName = secretName;
    environmentVariable.secretKey = secretKey;
    return environmentVariable;
  }
}

export class ApplicationConfig {
  id: number;
  name: string;
  config: string;
}

export class ApplicationVolume {
  id: number;
  name: string;
  mountPath: string;
  type: string;
  storageClassName = "standard";
  accessMode = "ReadWriteOnce";
  size: number;
  sizeStorageUnit = "Gi";
  secretName: string;
}

export class ApplicationSecret {
  id: number;
  name: string;
  data: { [key: string]: string };
}

export class ApplicationEnvironmentConfig {
  id: ApplicationEnvironmentConfigId;
  replicas: number;
  routeHostname: string;
  routeTargetPort: number;
  environmentVariables: ApplicationEnvironmentVariable[];
  configs: ApplicationConfig[];
  secrets: ApplicationSecret[];
}

export class ApplicationEnvironmentConfigId {
  environmentReleaseId: EnvironmentReleaseId;
  releaseVersionId: number;
  applicationVersionId: number;
}

export class ApplicationEnvironmentConfigIdUtil {

  public static toIdString(applicationEnvironmentConfig: ApplicationEnvironmentConfig): string {
    const id = applicationEnvironmentConfig.id;
    const erId = id.environmentReleaseId;
    return `${erId.environmentId}_${erId.releaseId}_${id.releaseVersionId}_${id.applicationVersionId}`;
  }

  public static toIdStringFromId(id: ApplicationEnvironmentConfigId): string {
    const erId = id.environmentReleaseId;
    return `${erId.environmentId}_${erId.releaseId}_${id.releaseVersionId}_${id.applicationVersionId}`;
  }
}

export class ApplicationReleaseStatus {
  id: ApplicationEnvironmentConfigId;
  status: string;
}
