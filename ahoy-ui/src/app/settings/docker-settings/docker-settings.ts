export class DockerSettings {
  id: number;
  dockerRegistries: DockerRegistry[];

  constructor(id: number) {
    this.id = id;
  }
}

export class DockerRegistry {
  id: number;
  name: string;
  server: string;
  username: string;
  password: string;
  secure: boolean;
}
