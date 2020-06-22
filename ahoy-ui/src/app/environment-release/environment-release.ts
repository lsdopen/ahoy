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
