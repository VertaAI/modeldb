export interface IArtifact {
  key: ArtifactKey;
  path: string;
}

export enum ArtifactKey {
  InputData = 'input_data',
  Model = 'model'
}

export class Artifact implements IArtifact {
  public readonly key: ArtifactKey;
  public readonly path: string;
  constructor(key: ArtifactKey, path: string) {
    this.key = key;
    this.path = path;
  }
}
