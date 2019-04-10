import User from 'models/User';

import { IAuthenticationService } from './IAuthenticationService';

export default class MockAuthenticationService
  implements IAuthenticationService {
  public user: User;

  constructor() {
    this.user = new User('testid', process.env.REACT_APP_USER_EMAIL);
    this.user.name = process.env.REACT_APP_USERNAME;
  }

  public async login(): Promise<User> {
    return this.user!;
  }

  public logout(): void {
    window.location.replace('/');
  }

  public async loadUser(): Promise<User> {
    return this.user!;
  }
}
