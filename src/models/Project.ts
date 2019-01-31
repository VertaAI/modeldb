export default class Project {
  private id: string = '';
  private name: string = '';
  private description: string = '';
  private date_created: Date = new Date();
  private date_updated: Date = new Date();
  private author?: string;
  private tags: Array<string> = [];

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
    return this.date_created;
  }

  public set DateCreated(v: Date) {
    this.date_created = v;
  }

  public get DateUpdated(): Date {
    return this.date_updated;
  }

  public set DateUpdated(v: Date) {
    this.date_updated = v;
  }

  public get Author(): string | undefined {
    return this.author;
  }

  public set Author(v: string | undefined) {
    this.author = v;
  }

  public get Tags(): Array<string> {
    return this.tags;
  }

  public set Tags(v: Array<string>) {
    this.tags = v;
  }
}
