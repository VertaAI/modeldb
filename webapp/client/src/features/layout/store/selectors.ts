import { ILayoutRootState } from './types';

export const selectIsCollapsedSidebar = (state: ILayoutRootState) =>
  state.layout.isCollapsedSidebar;
