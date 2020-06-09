import { IApplicationState } from '../../../setup/store/store';
import { ITagsManagerState } from './types';

const selectState = (state: IApplicationState): ITagsManagerState =>
  state.tagsManager;

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
