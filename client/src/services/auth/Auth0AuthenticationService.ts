import { Auth0DecodedHash, Auth0Error, Auth0UserProfile, WebAuth } from 'auth0-js';
import axios from 'axios';
import * as Cookies from 'es-cookie';
import { Jose, JoseJWS } from 'jose-jwe-jws';
import jwtDecode from 'jwt-decode';
import User from '../../models/User';
import { history } from '../../index';
import { AUTH_CONFIG } from './AuthConfiguration';
import { IAuthenticationService } from './IAuthenticationService';

interface IJwtToken {
  exp: number;
}

export default class Auth0AuthenticationService implements IAuthenticationService {
  private idTokenName: string = 'idToken';
  private auth0accessTokenName: string = 'auth0accessToken';
  private user: User | null = null;
  private auth0: WebAuth = new WebAuth({
    audience: 'https://verta.ai/api/v1',
    clientID: '',
    domain: '',
    redirectUri: '',
    responseType: 'id_token token',
    scope: 'openid profile email'
  });

  constructor() {
    this.getProfile = this.getProfile.bind(this);
    this.handleAuthentication = this.handleAuthentication.bind(this);
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
    this.setSession = this.setSession.bind(this);
  }

  get accessToken(): string {
    const accessToken = Cookies.get(this.auth0accessTokenName);
    return '';
  }

  get idToken(): string {
    const idToken = Cookies.get(this.idTokenName);
    if (!idToken) {
      throw new Error('No id token found');
    }
    return idToken;
  }

  get authenticated(): boolean {
    const cookie = Cookies.get(this.idTokenName);
    if (cookie) {
      const expiresAt = jwtDecode<IJwtToken>(cookie).exp;
      return new Date().getTime() < expiresAt * 1000;
    }
    return false;
  }

  public login(): void {
    // this.auth0.authorize();
    // call the backend server
    // console.log('called login on Auth0AuthenticationService');
    // axios.get('/api/auth/login', { crossdomain: true });
    console.log('adf');
    window.location.replace('/api/auth/login');
    // history.push('api/auth/login');
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

  public async loadUser(): Promise<User> {
    const res = await axios.get<User>('/api/getUser');
    return res.data;
  }

  public getProfile(): Promise<User> {
    return new Promise((resolve, reject) => {
      if (this.user && this.user.email) {
        return resolve(this.user);
      }
      const accessToken = this.accessToken;
      this.auth0.client.userInfo(accessToken, (error: Auth0Error | null, auth0User: Auth0UserProfile) => {
        if (error) {
          reject(error);
        } else {
          this.user = this.convertAuth0UserToUser(auth0User);
          resolve(this.user);
        }
      });
    });
  }

  public async setSession(authResult: Auth0DecodedHash): Promise<void> {
    const { idToken, accessToken } = authResult;

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
          Cookies.set(this.idTokenName, idToken!, { expires: new Date(token.exp * 1000) });
          Cookies.set(this.auth0accessTokenName, accessToken!, { expires: new Date(token.exp * 1000) });
          const auth0User = jwtDecode<Auth0UserProfile>(idToken!);
          this.user = this.convertAuth0UserToUser(auth0User);
        });
      }
    });
  }

  public logout(): void {
    Cookies.remove(this.idTokenName);
    Cookies.remove(this.auth0accessTokenName);
    this.user = null;
  }

  private convertAuth0UserToUser(auth0User: Auth0UserProfile): User {
    const user = new User(auth0User.user_id || auth0User.sub, auth0User.email!);
    user.name = auth0User.name;
    user.picture = auth0User.picture;
    return user;
  }
}
