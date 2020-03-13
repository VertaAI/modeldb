import { JsonObject, JsonProperty } from 'json2typescript';

import { Markdown } from 'core/shared/utils/types';

import * as CodeVersion from 'core/shared/models/CodeVersion';
import * as Common from 'core/shared/models/Common';
import * as Workspace from './Workspace';

@JsonObject('project')
export class Project
  implements Common.IEntityWithLogging, Workspace.IEntityWithShortWorkspace {
  @JsonProperty('id', String)
  public id: string;
  @JsonProperty('name', String, true)
  public name: string;
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('tags', [String], true)
  public tags: string[] = [];
  @JsonProperty('readme_text', String, true)
  public readme: Markdown = '';
  public codeVersion?: CodeVersion.ICodeVersion = undefined;
  public dateCreated: Date = new Date();
  public dateUpdated: Date = new Date();

  public shortWorkspace: Workspace.IShortWorkspace = {
    id: '' as any,
    type: 'user',
  };

  public constructor(id?: string, name?: string) {
    this.id = id || '';
    this.name = name || '';
  }
}

export interface IProjectCreationSettings {
  name: string;
  visibility: ProjectVisibility;
  tags?: string[];
  description?: string;
}

export const projectAlreadyExistsError = 'projectAlreadyExists';

export const ProjectVisibility = {
  private: 'private',
  // enabled after there is backend support
  // public: 'public',
};
export type ProjectVisibility = keyof typeof ProjectVisibility;
