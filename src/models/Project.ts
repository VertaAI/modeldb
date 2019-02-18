import User from './User';
export enum UserAccess {
  Owner = 0,
  Write = 1,
  Read = 2
}

export default class Project {
  private id: string;
  private name: string;
  private description: string = '';
  private dateCreated: Date = new Date();
  private dateUpdated: Date = new Date();
  private author: User;
  private collaborators: Map<User, UserAccess> = new Map<User, UserAccess>();
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

  public get Collaborators(): Map<User, UserAccess> {
    return this.collaborators;
  }

  public set Collaborators(v: Map<User, UserAccess>) {
    this.collaborators = v;
  }

  public get Tags(): string[] {
    return this.tags;
  }

  public set Tags(v: string[]) {
    this.tags = v;
  }

  public get Author(): User {
    return this.author;
  }

  public set Author(v: User) {
    this.author = v;
  }

  constructor(id: string, name: string, author: User) {
    this.id = id;
    this.name = name;
    this.author = author;

    this.Collaborators.set(author, UserAccess.Owner);
  }
}
