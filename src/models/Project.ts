export default class Project {
  private id: string = '';
  private name: string = '';
  private description: string = '';
  private creation_date: Date = new Date();
  private updated_date: Date = new Date();
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

  public get CreationDate(): Date {
    return this.creation_date;
  }

  public set CreationDate(v: Date) {
    this.creation_date = v;
  }

  public get UpdatedDate(): Date {
    return this.updated_date;
  }

  public set UpdatedDate(v: Date) {
    this.updated_date = v;
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
