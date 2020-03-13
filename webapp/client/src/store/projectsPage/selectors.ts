import { IApplicationState } from '../store';
import { IProjectsPageState } from './types';

const selectState = (state: IApplicationState): IProjectsPageState =>
  state.projectsPage;

export const selectIsShowDeveloperKeyInfo = (state: IApplicationState) =>
  selectState(state).data.isShowDeveloperKeyInfo;
