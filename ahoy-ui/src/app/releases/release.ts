import {ApplicationVersion} from '../applications/application';

export class Release {
  id: number;
  name: string;
  releaseVersions: ReleaseVersion[];
}

export class ReleaseVersion {
  id: number;
  version: string;
  release: Release | string;
  applicationVersions: ApplicationVersion[] = undefined;
}
