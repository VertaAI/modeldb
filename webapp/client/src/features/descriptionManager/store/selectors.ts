import { IApplicationState } from '../../../setup/store/store';
import { IDescriptionManagerState } from './types';

const selectState = (state: IApplicationState): IDescriptionManagerState =>
  state.descriptionManager;

export const selectIsAddingDesc = (state: IApplicationState) =>
  selectCommunications(state).addingDesc.isRequesting;

export const selectIsEditingDesc = (state: IApplicationState) =>
  selectCommunications(state).editingDesc.isRequesting;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectIsUpdatingDesc = (state: IApplicationState) => {
  const communications = selectCommunications(state);
  return (
    communications.addingDesc.isRequesting ||
    communications.editingDesc.isRequesting
  );
};
