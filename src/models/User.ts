export default class User {
  public readonly id: number | undefined;
  public readonly name: string | undefined;
  public readonly avatarUrl: string | undefined;

  public constructor(id: number, name: string, avatarUrl?: string) {
    this.id = id;
    this.name = name;
    this.avatarUrl = avatarUrl;
  }
}
