import { IApplicationState } from '../store';
import { IDescriptionActionState } from './types';

const selectState = (state: IApplicationState): IDescriptionActionState =>
  state.descriptionAction;

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
