import { UserAccess } from './Project';

export default class User {
  public id: string | undefined;
  public name?: string | undefined;
  public email: string;
  public picture?: string | undefined;
  public access: UserAccess = UserAccess.Read;

  public constructor(id: string | undefined, email: string) {
    this.id = id;
    this.email = email;
  }

  public getNameOrEmail(): string {
    return this.name ? this.name : this.email;
  }
}

export class Collaborator extends User {
  public access: UserAccess;

  public constructor(
    id: string | undefined,
    email: string,
    access: UserAccess
  ) {
    super(id, email);
    this.access = access;
  }
}

// tslint:disable-next-line: max-classes-per-file
export class CurrentUser extends User {
  public dateLastLoggedIn: Date;
  public developerKey: string;

  public constructor(opts: {
    id: string | undefined;
    email: string;
    dateLastLoggedIn: Date;
    developerKey: string;
  }) {
    const { id, email, dateLastLoggedIn, developerKey } = opts;
    super(id, email);
    this.dateLastLoggedIn = dateLastLoggedIn;
    this.developerKey = developerKey;
  }
}
