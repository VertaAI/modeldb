import { Auth0DecodedHash, Auth0Error, WebAuth } from 'auth0-js';
import * as Cookies from 'es-cookie';
import { Jose, JoseJWS } from 'jose-jwe-jws';
import jwtDecode from 'jwt-decode';
import User from '../../models/User';
import { AUTH_CONFIG } from './AuthConfiguration';
import { IAuthenticationService } from './IAuthenticationService';

interface IJwtToken {
  exp: number;
}

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
    throw new Error('No access token found');
  }

  get idToken(): string {
    const idToken = Cookies.get('id_token');
    if (!idToken) {
      throw new Error('No id token found');
    }
    return idToken;
  }

  get authenticated(): boolean {
    const cookie = Cookies.get('id_token');
    if (cookie) {
      const expiresAt = jwtDecode<IJwtToken>(cookie).exp;
      return new Date().getTime() < expiresAt * 1000;
    }
    return false;
  }

  public login(): void {
    this.auth0.authorize();
  }

  public async handleAuthentication(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.auth0.parseHash(async (e: Auth0Error | null, result: Auth0DecodedHash | null) => {
        if (result && result.accessToken && result.idToken) {
          resolve(await this.setSession(result));
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

  public async setSession(authResult: Auth0DecodedHash): Promise<void> {
    const { idToken } = authResult;

    await fetch(`https://${AUTH_CONFIG.domain}/.well-known/jwks.json`).then(async res => {
      const jsonResponse = await res.json();

      const cryptographer = new Jose.WebCryptographer();
      cryptographer.setContentSignAlgorithm('RS256');
      const verifier = new JoseJWS.Verifier(cryptographer, idToken!);
      const jwkrsas = jsonResponse.keys as JWKRSA[];
      for (const jwkrsa of jwkrsas) {
        await verifier.addRecipient(jwkrsa, jwkrsa.kid, 'RS256');
        await verifier.verify().then(result => {
          const token = jwtDecode<IJwtToken>(idToken!);
          Cookies.set('id_token', idToken!, { expires: new Date(token.exp * 1000) });
          this.user = jwtDecode<User>(idToken!);
        });
      }
    });
  }

  public logout(): void {
    Cookies.remove('id_token');
    this.user = null;
  }
}
