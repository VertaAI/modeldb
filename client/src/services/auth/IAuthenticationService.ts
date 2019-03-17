import { Auth0DecodedHash } from 'auth0-js';
import User from '../../models/User';

export interface IAuthenticationService {
  readonly authenticated: boolean;
  readonly accessToken: string;
  readonly idToken: string;
  login(): void;
  handleAuthentication(): Promise<void>;
  getProfile(): Promise<User>;
  setSession(authResult: Auth0DecodedHash): Promise<void>;
  logout(): void;
}
