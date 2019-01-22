export interface IArtifact {
  key: ArtifactKey;
  path: string;
  artifactType: ArtifactType;
}

export enum ArtifactType {
  Data = 'DATA',
  Model = 'MODEL'
}

export enum ArtifactKey {
  InputData = 'input_data',
  Model = 'model'
}

export class Artifact implements IArtifact {
  public readonly key: ArtifactKey;
  public readonly path: string;
  public readonly artifactType: ArtifactType;
  constructor(key: ArtifactKey, path: string, artifactType: ArtifactType) {
    this.key = key;
    this.path = path;
    this.artifactType = artifactType;
  }
}
