import { JsonObject, JsonProperty } from 'json2typescript';

export interface IArtifact {
  key: string;
  path: string;
  type: string;
}

@JsonObject('artifact')
export class Artifact implements IArtifact {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('path', String)
  public readonly path: string;
  @JsonProperty('artifact_type', String, true)
  public readonly type: string;

  public constructor(key?: string, path?: string, type: string = 'IMAGE') {
    this.key = key || '';
    this.path = path || '';
    this.type = type || '';
  }
}
