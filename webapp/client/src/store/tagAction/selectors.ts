import { IApplicationState } from '../store';
import { ITagActionState } from './types';

const selectState = (state: IApplicationState): ITagActionState =>
  state.tagAction;

export const selectIsAddingTags = (state: IApplicationState) =>
  selectCommunications(state).addingTag.isRequesting;

export const selectIsRemovingTags = (state: IApplicationState) =>
  selectCommunications(state).removingTag.isRequesting;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectIsUpdatingTags = (state: IApplicationState) => {
  const communications = selectCommunications(state);
  return (
    communications.addingTag.isRequesting ||
    communications.removingTag.isRequesting
  );
};
