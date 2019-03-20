import User from '../../models/User';
import { IAuthenticationService } from './IAuthenticationService';

export default class MockAuthenticationService implements IAuthenticationService {
  public user: User | null;

  constructor() {
    this.user = new User('testid', 'Manasi.Vartak@verta.ai');
    this.user.name = 'Manasi Vartak';
  }

  public login(): void {}

  public logout(): void {}

  public loadUser(): any {
    return {};
  }
}
