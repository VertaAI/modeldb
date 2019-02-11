import { IMetaData, MetaData } from './IMetaData';
import User from './User';

export default class Project {
  public static metaData: IMetaData[] = [{ propertyName: 'Name' }, { propertyName: 'Description' }];
  private id: string = '';
  private name: string = '';
  private description: string = '';
  private dateCreated: Date = new Date();
  private dateUpdated: Date = new Date();
  private author?: User | undefined;
  private tags: string[] = [];

  public get Id(): string {
    return this.id;
  }

  public set Id(v: string) {
    this.id = v;
  }

  public get Name(): string {
    return this.name;
  }

  public set Name(v: string) {
    this.name = v;
  }

  public get Description(): string {
    return this.description;
  }

  public set Description(v: string) {
    this.description = v;
  }

  public get DateCreated(): Date {
    return this.dateCreated;
  }

  public set DateCreated(v: Date) {
    this.dateCreated = v;
  }

  public get DateUpdated(): Date {
    return this.dateUpdated;
  }

  public set DateUpdated(v: Date) {
    this.dateUpdated = v;
  }

  public get Author(): User | undefined {
    return this.author;
  }

  public set Author(v: User | undefined) {
    this.author = v;
  }

  public get Tags(): string[] {
    return this.tags;
  }

  public set Tags(v: string[]) {
    this.tags = v;
  }
}
