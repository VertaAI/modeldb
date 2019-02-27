export default class User {
  public id: string | undefined;
  public name?: string | undefined;
  public email: string;
  public picture?: string | undefined;

  public constructor(id: string | undefined, email: string) {
    this.id = id;
    this.email = email;
  }

  public getNameOrEmail(): string {
    return this.name ? this.name : this.email;
  }
}
