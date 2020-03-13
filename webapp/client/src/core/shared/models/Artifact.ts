import { JsonObject, JsonProperty } from 'json2typescript';

export interface IArtifact {
  key: string;
  path: string;
  type: ArtifactType;
  pathOnly: boolean;
  linkedArtifactId?: string;
  fileExtension?: string;
}
export interface IArtifactWithPath extends IArtifact {
  pathOnly: false;
}

export const checkArtifactWithPath = (
  artifact: IArtifact
): artifact is IArtifactWithPath => {
  return Boolean(artifact.path && !artifact.pathOnly);
};

@JsonObject('artifact')
export class Artifact implements IArtifact {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('path', String)
  public readonly path: string;
  @JsonProperty('artifact_type', String, true)
  public readonly type: ArtifactType;
  @JsonProperty('path_only', Boolean, true)
  public readonly pathOnly: boolean;
  @JsonProperty('linked_artifact_id', String, true)
  public linkedArtifactId?: string = '';
  @JsonProperty('filename_extension', String, true)
  public fileExtension?: string = undefined;

  public constructor(
    pathOnly: boolean,
    type: ArtifactType,
    key: string,
    path?: string
  ) {
    this.key = key || '';
    this.path = path || '';
    this.type = type || 'BLOB';
    this.pathOnly = pathOnly || false;
  }
}

export type ArtifactType =
  | 'IMAGE'
  | 'BLOB'
  | 'BINARY'
  | 'QUERY'
  | 'DATA'
  | 'CODE';
