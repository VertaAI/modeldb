import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from '../utils/MapperConverters';
import User from './User';

export enum UserAccess {
  Owner = 0,
  Write = 1,
  Read = 2
}

@JsonObject('project')
export class Project {
  @JsonProperty('id', String)
  public id: string;
  @JsonProperty('name', String)
  public name: string;
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('date_created', StringToDateConverter, true)
  public dateCreated: Date = new Date();
  @JsonProperty('date_updated', StringToDateConverter, true)
  public dateUpdated: Date = new Date();
  public collaborators: Map<User, UserAccess> = new Map<User, UserAccess>();
  @JsonProperty('tags', [String], true)
  public tags: string[] = [];
  @JsonProperty('owner', String)
  public authorId: string;
  private author: User = new User('', '');

  public constructor(id?: string, name?: string, authorId?: string) {
    this.id = id || '';
    this.name = name || '';
    this.authorId = authorId || '';
  }

  public get Author(): User {
    return this.author;
  }

  public set Author(v: User) {
    this.collaborators.set(v, UserAccess.Owner);
    this.author = v;
  }
}
