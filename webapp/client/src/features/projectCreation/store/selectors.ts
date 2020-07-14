import { IApplicationState } from '../../../setup/store/store';
import { IProjectCreationState } from './types';

const selectState = (state: IApplicationState): IProjectCreationState =>
  state.projectCreation;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;
