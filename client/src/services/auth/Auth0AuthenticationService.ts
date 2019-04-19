import axios from 'axios';
import { bind } from 'decko';
import { JsonConvert } from 'json2typescript';

import { CurrentUser } from 'models/User';

import { IAuthenticationService } from './IAuthenticationService';

export default class Auth0AuthenticationService
  implements IAuthenticationService {
  @bind
  public login(): void {
    window.location.replace('/api/auth/login');
  }

  @bind
  public async loadUser(): Promise<CurrentUser> {
    const res = await axios.get<any>('/api/auth/getUser');
    const serverUser = res.data;
    // todo refactor
    const user = new CurrentUser({
      id: serverUser.sub,
      email: serverUser.email,
      dateLastLoggedIn: new Date(serverUser.updated_at),
      developerKey: serverUser.developer_key,
    });
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
