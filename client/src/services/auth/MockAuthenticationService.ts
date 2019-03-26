import { history } from 'index';
import User from 'models/User';

import { IAuthenticationService } from './IAuthenticationService';

export default class MockAuthenticationService implements IAuthenticationService {
  public jwtToken: string = '';
  public authenticated: boolean = false;
  public accessToken: string = '';
  public user: User;

  public constructor() {
    this.user = new User('testid', process.env.REACT_APP_USER_EMAIL);
    this.user.name = process.env.REACT_APP_USERNAME;
  }

  public login(): void {
    history.push('/callback');
    this.authenticated = true;
  }

  public handleAuthentication(): Promise<void> {
    return Promise.resolve();
  }

  public getProfile(): Promise<User> {
    return new Promise<User>((resolve, reject) => {
      resolve(this.user!);
    });
  }

  public logout(): void {
    this.authenticated = false;
  }
}
