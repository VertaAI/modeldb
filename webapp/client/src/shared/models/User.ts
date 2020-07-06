import { Brand } from 'shared/utils/Brand';
import {
  combineValidators,
  validateNotEmpty,
  validateLengthLessThen,
  validateSymbols,
} from 'shared/utils/validators';

export interface IUserConstructorOptions {
  id: string;
  email: string;
  username: string;
  fullName?: string;
  picture?: string;
}
declare const username: unique symbol;
export type Username = Brand<string, 'username', typeof username>;

declare const userId: unique symbol;
export type UserId = Brand<string, 'userId', typeof userId>;

export default class User {
  public id: UserId;
  public email: string;
  public username: Username;
  public fullName?: string;
  public picture?: string;

  public constructor(opts: IUserConstructorOptions) {
    this.id = opts.id as UserId;
    this.email = opts.email;
    this.username = opts.username as Username;
    this.fullName = opts.fullName;
    this.picture = opts.picture;
  }

  public getEmail(): string {
    return this.email ? this.email : 'No email specified';
  }

  public getName(): string {
    return this.fullName ? this.fullName : 'No name specified';
  }
}

export class CurrentUser extends User {
  public dateLastLoggedIn: Date;
  public developerKey: string;

  public constructor(
    opts: IUserConstructorOptions & {
      dateLastLoggedIn: Date;
      developerKey: string;
    }
  ) {
    super(opts);
    this.dateLastLoggedIn = opts.dateLastLoggedIn;
    this.developerKey = opts.developerKey;
  }
}

export const UpdateUsernameErrors = {
  usernameAlreadyExist: 'usernameAlreadyExist',
} as const;
export type UpdateUsernameError = (typeof UpdateUsernameErrors)[keyof (typeof UpdateUsernameErrors)];

export enum AuthError {
  accessDenied = 'access_denied',
  userNotVerified = 'user_not_verified',
  unknown = 'unknown',
}

export const validateUserName = (value: string): string | undefined => {
  const validateLength = validateLengthLessThen(25, 'user name');

  return combineValidators([
    validateNotEmpty('username'),
    validateSymbols(['alphanumeric chars', 'dash', 'underscore']),
    validateLength,
  ])(value);
};

export const unknownUser = new User({
  id: '',
  email: 'unknown',
  username: 'unknown',
  fullName: 'unknown',
  picture: '',
});
