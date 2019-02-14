export default class User {
  public name?: string | undefined;
  public email: string;
  public picture?: string | undefined;

  public constructor(email: string) {
    this.email = email;
  }

  public getNameOrEmail(): string {
    return this.name ? this.name : this.email;
  }
}
