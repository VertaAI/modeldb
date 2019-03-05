export interface IArtifact {
  key: string;
  path: string;
  artifactType: string;
}

export class Artifact implements IArtifact {
  public readonly key: string;
  public readonly path: string;
  public readonly artifactType: string = 'IMAGE';
  constructor(key: string, path: string, artifactType: string) {
    this.key = key;
    this.path = path;
    if (artifactType !== undefined) {
      this.artifactType = artifactType;
    }
  }
}
