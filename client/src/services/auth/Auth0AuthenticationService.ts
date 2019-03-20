import axios from 'axios';
import User from '../../models/User';
import { IAuthenticationService } from './IAuthenticationService';

export default class Auth0AuthenticationService implements IAuthenticationService {
  constructor() {
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
  }

  public login(): void {
    window.location.replace('/api/auth/login');
  }

  public async loadUser(): Promise<User> {
    const res = await axios.get<User>('/api/getUser');
    return res.data;
  }

  public async logout() {
    try {
      await axios.get('auth/logout');
    } catch {
    }
  }
}
