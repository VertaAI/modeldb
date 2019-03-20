import { Auth0DecodedHash } from 'auth0-js';
import User from '../../models/User';

export interface IAuthenticationService {
  login(): void;
  logout(): void;
  loadUser(): Promise<User>;
}
