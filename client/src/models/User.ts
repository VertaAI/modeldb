export default class User {
  public id: string | undefined;
  public name?: string | undefined;
  public email: string;
  public picture?: string | undefined;
  public dateLastLoggedIn?: Date = new Date();
  public developerKey = 'A3afe33453fvfdd4DFVgssgg3';

  public constructor(id: string | undefined, email: string) {
    this.id = id;
    this.email = email;
    this.dateLastLoggedIn = new Date();
  }

  public getNameOrEmail(): string {
    return this.name ? this.name : this.email;
  }
}
