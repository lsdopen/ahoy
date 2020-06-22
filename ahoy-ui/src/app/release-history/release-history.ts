import {EnvironmentRelease} from '../environment-release/environment-release';
import {ReleaseVersion} from '../releases/release';

export class ReleaseHistory {
  id: number;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  action: string;
  status: string;
  time: Date;
  description: string;
}
