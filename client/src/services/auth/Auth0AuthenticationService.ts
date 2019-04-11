import axios from 'axios';
import { bind } from 'decko';
import { JsonConvert } from 'json2typescript';

import User from 'models/User';

import { IAuthenticationService } from './IAuthenticationService';

export default class Auth0AuthenticationService
  implements IAuthenticationService {
  @bind
  public login(): void {
    window.location.replace('/api/auth/login');
  }

  @bind
  public async loadUser(): Promise<User> {
    const res = await axios.get<User>('/api/getUser');
    const serverUser = res.data;
    const user = new User(serverUser.id, serverUser.email);
    user.email = serverUser.email;
    user.name = serverUser.name;
    user.picture = serverUser.picture;

    return user;
  }

  @bind
  public async logout() {
    try {
      await axios.get('auth/logout');
    } catch {}
  }
}
