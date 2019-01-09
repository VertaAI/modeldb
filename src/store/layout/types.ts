export type ThemeColors = 'light' | 'dark';

export enum LayoutActionTypes {
  SET_THEME = '@@layout/SET_THEME',
}

export interface ILayoutState {
  readonly theme: ThemeColors;
}
