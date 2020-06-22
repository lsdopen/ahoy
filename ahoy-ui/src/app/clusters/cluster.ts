export class Cluster {
  id: number;
  name: string;
  masterUrl: string;
  token: string;
  caCertData: string;
  host: string;
  dockerRegistry: string;
  type: string;
  inCluster = false;
}
