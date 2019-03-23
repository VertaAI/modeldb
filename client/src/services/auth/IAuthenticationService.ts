import User from 'models/User';

export interface IAuthenticationService {
  readonly authenticated: boolean;
  readonly accessToken: string;
  readonly jwtToken: string;
  readonly user: User;
  login(): void;
  handleAuthentication(): Promise<void>;
  getProfile(): Promise<User>;
  logout(): void;
}
