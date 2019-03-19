import { Auth0DecodedHash } from 'auth0-js';
import User from '../../models/User';
import { IAuthenticationService } from './IAuthenticationService';

export default class MockAuthenticationService implements IAuthenticationService {
  public idToken: string = '';
  public authenticated: boolean = false;
  public accessToken: string = '';
  public user: User | null;

  constructor() {
    this.user = new User('testid', 'Manasi.Vartak@verta.ai');
    this.user.name = 'Manasi Vartak';
  }

  public login(): void {
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

  public setSession(authResult: Auth0DecodedHash): Promise<void> {
    return Promise.resolve();
  }

  public logout(): void {
    this.authenticated = false;
  }

  public loadUser(): any {
    return {};
  }
}
