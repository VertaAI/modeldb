import { Auth0DecodedHash, Auth0Error, WebAuth } from 'auth0-js';
import jwtDecode from 'jwt-decode';
import User from '../../models/User';
import { AUTH_CONFIG } from './AuthConfiguration';
import { IAuthenticationService } from './IAuthenticationService';

export default class Auth0AuthenticationService implements IAuthenticationService {
  private storage: Storage = sessionStorage;
  private user: User | null = null;
  private auth0: WebAuth = new WebAuth({
    audience: `https://${AUTH_CONFIG.domain}/userinfo`,
    clientID: AUTH_CONFIG.clientId,
    domain: AUTH_CONFIG.domain,
    redirectUri: AUTH_CONFIG.callbackUrl,
    responseType: 'id_token token',
    scope: 'openid profile'
  });

  constructor() {
    this.getProfile = this.getProfile.bind(this);
    this.handleAuthentication = this.handleAuthentication.bind(this);
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
    this.setSession = this.setSession.bind(this);
  }

  get accessToken(): string {
    const accessToken = this.storage.getItem('access_token');
    if (!accessToken) {
      throw new Error('No access token found');
    }
    return accessToken;
  }

  get idToken(): string {
    const idToken = this.storage.getItem('id_token');
    if (!idToken) {
      throw new Error('No id token found');
    }
    return idToken;
  }

  get authenticated(): boolean {
    const expiresAt = JSON.parse(this.storage.getItem('expires_at')!);
    return new Date().getTime() < expiresAt;
  }

  public login(): void {
    this.auth0.authorize();
  }

  public handleAuthentication(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.auth0.parseHash((e: Auth0Error | null, result: Auth0DecodedHash | null) => {
        if (result && result.accessToken && result.idToken) {
          resolve(this.setSession(result));
        }
        if (e) {
          reject(e.error);
        }
      });
    });
  }

  public getProfile(): Promise<User> {
    return new Promise((resolve, reject) => {
      if (this.user) {
        return resolve(this.user);
      }
      const accessToken = this.accessToken;
      this.auth0.client.userInfo(accessToken, (error: Auth0Error | null, user: User) => {
        if (error) {
          reject(error);
        } else {
          this.user = user;
          resolve(this.user);
        }
      });
    });
  }

  public setSession(authResult: Auth0DecodedHash): void {
    const { accessToken, expiresIn, idToken } = authResult;
    const expiresAt = JSON.stringify(expiresIn! * 1000 + new Date().getTime());
    this.storage.setItem('access_token', accessToken!);
    this.storage.setItem('expires_at', expiresAt);

    this.storage.setItem('id_token', idToken!);
    this.user = jwtDecode<User>(idToken!);
  }

  public logout(): void {
    this.storage.removeItem('access_token');
    this.storage.removeItem('id_token');
    this.storage.removeItem('expires_at');
    this.user = null;
  }
}
