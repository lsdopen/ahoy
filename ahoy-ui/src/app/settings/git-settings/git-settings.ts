export class GitSettings {
  id: number;
  remoteRepoUri: string;
  httpsUsername: string;
  httpsPassword: string;
  credentials = 'NONE';
  privateKey: string;
  sshKnownHosts: string;

  constructor(id: number) {
    this.id = id;
  }
}
