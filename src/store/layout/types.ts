import User from 'models/User';

export enum LayoutActionTypes {
  SET_THEME = '@@layout/SET_THEME'
}

export interface ILayoutState {
  readonly user: User | undefined;
}
