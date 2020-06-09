import { JsonObject, JsonProperty } from 'json2typescript';

import * as CodeVersion from 'core/shared/models/CodeVersion';
import * as Common from 'core/shared/models/Common';

@JsonObject('experiment')
export default class Experiment implements Common.IEntityWithLogging {
  @JsonProperty('id', String, true)
  public id: string = '';
  @JsonProperty('project_id', String, true)
  public projectId: string = '';
  @JsonProperty('name', String, true)
  public name: string = '';
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('tags', [String], true)
  public tags: string[] = [];
  public dateCreated: Date = new Date();
  public dateUpdated: Date = new Date();
  public codeVersion?: CodeVersion.ICodeVersion = undefined;
}

@JsonObject('shortExperiment')
export class ShortExperiment {
  @JsonProperty('id', String, true)
  public id: string = '';
  @JsonProperty('name', String, true)
  public name: string = '';
}

export interface IExperimentCreationSettings {
  name: string;
  tags?: string[];
  description?: string;
}
