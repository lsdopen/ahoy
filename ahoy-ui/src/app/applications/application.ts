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
  environmentVariables: { [key: string]: string };
  healthEndpointPath: string;
  healthEndpointPort: number;
  healthEndpointScheme: string;
  servicePorts: number[];
  application: Application | string;
  configs: ApplicationConfig[];
  configPath: string;
  environmentConfig: ApplicationEnvironmentConfig;
  status: ApplicationReleaseStatus;
}

export class ApplicationConfig {
  id: number;
  name: string;
  config: string;
}

export class ApplicationEnvironmentConfig {
  id: ApplicationEnvironmentConfigId;
  replicas: number;
  routeHostname: string;
  routeTargetPort: number;
  environmentVariables: { [key: string]: string };
  configFileName: string;
  configFileContent: string;
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
