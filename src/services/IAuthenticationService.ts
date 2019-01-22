import User from '../models/User';

export interface IAuthenticationService {
  authenticate(): Promise<User>;
}
