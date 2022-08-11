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

import {EnvironmentReleaseId} from '../environment-release/environment-release';
import {ReleaseVersion} from '../releases/release';
import {Nameable} from '../components/name-unique-validator.directive';

export class Application {
  id: number;
  name: string;
  applicationVersions: ApplicationVersion[];
  latestApplicationVersion: ApplicationVersion;
}

export class ApplicationVersion {
  id: number;
  version: string;
  spec: ApplicationSpec;
  application: Application | string;
  status: ApplicationReleaseStatus;
  releaseVersions: ReleaseVersion[];
}

export class ContainerSpec implements Nameable {
  name: string;
  image: string;
  type = 'Container';
  commandArgsEnabled = false;
  command: string;
  args: string[] = [];
  servicePortsEnabled = false;
  servicePorts: number[] = [];
  healthChecksEnabled = false;
  livenessProbe = new ApplicationProbe(60, 10, 5, 1, 3);
  readinessProbe = new ApplicationProbe(10, 10, 5, 1, 3);
  environmentVariablesEnabled = false;
  environmentVariables: ApplicationEnvironmentVariable[] = [];
  resourcesEnabled = false;
  resources = new ApplicationResources();
}

export class ApplicationSpec extends ContainerSpec {
  dockerRegistryName: string;
  configPath: string;
  configFilesEnabled = false;
  configFiles: ApplicationConfigFile[] = [];
  volumesEnabled = false;
  volumes: ApplicationVolume[] = [];
  secretsEnabled = false;
  secrets: ApplicationSecret[] = [];
  containers: ContainerSpec[] = [];
}

export class ApplicationEnvironmentVariable {
  key: string;
  value: string;
  type: string;
  secretName: string;
  secretKey: string;

  static newValueType(key: string, value: string): ApplicationEnvironmentVariable {
    const environmentVariable = new ApplicationEnvironmentVariable();
    environmentVariable.type = 'Value';
    environmentVariable.key = key;
    environmentVariable.value = value;
    return environmentVariable;
  }

  static newSecretType(key: string, secretName: string, secretKey: string): ApplicationEnvironmentVariable {
    const environmentVariable = new ApplicationEnvironmentVariable();
    environmentVariable.type = 'Secret';
    environmentVariable.key = key;
    environmentVariable.secretName = secretName;
    environmentVariable.secretKey = secretKey;
    return environmentVariable;
  }
}

export class ApplicationConfigFile {
  name: string;
  content: string;
}

export class ApplicationVolume {
  name: string;
  mountPath: string;
  type: string;
  storageClassName;
  accessMode = 'ReadWriteOnce';
  size: number;
  sizeStorageUnit = 'Gi';
  secretName: string;
}

export class ApplicationSecret {
  name: string;
  type: string;
  data: { [key: string]: string };
}

export class ApplicationEnvironmentConfig {
  id: ApplicationEnvironmentConfigId;
  spec: ApplicationEnvironmentSpec;
}

export class ApplicationEnvironmentSpec {
  replicas = 1;
  routeEnabled = false;
  routes: ApplicationRoute[] = [];
  tls: boolean;
  tlsSecretName: string;
  environmentVariablesEnabled = false;
  environmentVariables: ApplicationEnvironmentVariable[] = [];
  configFilesEnabled = false;
  configFiles: ApplicationConfigFile[] = [];
  volumesEnabled = false;
  volumes: ApplicationVolume[] = [];
  secretsEnabled = false;
  secrets: ApplicationSecret[] = [];
  resourcesEnabled = false;
  resources = new ApplicationResources();
}

export class ApplicationRoute {
  hostname: string;
  targetPort: number;

  constructor(hostname: string, targetPort: number) {
    this.hostname = hostname;
    this.targetPort = targetPort;
  }
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

export class ApplicationResources {
  limitCpu: number;
  limitMemory: number;
  limitMemoryUnit = 'Mi';

  requestCpu: number;
  requestMemory: number;
  requestMemoryUnit = 'Mi';
}

export class ApplicationProbe {
  httpGet = new HttpEndpoint();
  initialDelaySeconds: number;
  periodSeconds: number;
  timeoutSeconds: number;
  successThreshold: number;
  failureThreshold: number;

  constructor(initialDelaySeconds: number, periodSeconds: number, timeoutSeconds: number, successThreshold: number, failureThreshold: number) {
    this.initialDelaySeconds = initialDelaySeconds;
    this.periodSeconds = periodSeconds;
    this.timeoutSeconds = timeoutSeconds;
    this.successThreshold = successThreshold;
    this.failureThreshold = failureThreshold;
  }
}

export class HttpEndpoint {
  path: string;
  port: number;
  scheme = 'HTTP';
}
