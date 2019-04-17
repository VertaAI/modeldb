import { CurrentUser } from 'models/User';

import { IAuthenticationService } from './IAuthenticationService';

export default class MockAuthenticationService
  implements IAuthenticationService {
  public user: CurrentUser;

  constructor() {
    this.user = new CurrentUser({
      dateLastLoggedIn: new Date(),
      developerKey: 'adf313adfadf',
      email: process.env.REACT_APP_USER_EMAIL,
      id: 'testId',
    });
    this.user.name = process.env.REACT_APP_USERNAME;
  }

  public async login(): Promise<CurrentUser> {
    return this.user!;
  }

  public logout(): void {
    window.location.replace('/');
  }

  public async loadUser(): Promise<CurrentUser> {
    return this.user!;
  }
}
