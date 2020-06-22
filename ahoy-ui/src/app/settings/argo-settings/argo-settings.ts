export class ArgoSettings {
  id: number;
  argoServer: string;
  argoToken: string;

  constructor(id: number) {
    this.id = id;
  }
}
