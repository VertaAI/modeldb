import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from 'utils/MapperConverters';
import User from './User';

@JsonObject('project')
export class Project {
  @JsonProperty('id', String)
  public id: string;
  @JsonProperty('name', String, true)
  public name: string;
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('date_created', StringToDateConverter, true)
  public dateCreated: Date = new Date();
  @JsonProperty('date_updated', StringToDateConverter, true)
  public dateUpdated: Date = new Date();
  @JsonProperty('tags', [String], true)
  public tags: string[] = [];
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
    this.author = v;
  }
}
