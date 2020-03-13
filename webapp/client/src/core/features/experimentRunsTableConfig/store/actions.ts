import {
  IToggleColumnVisibilityAction,
  toggleColumnVisibilityActionTypes,
} from './types';

export const toggleColumnVisibility = (
  payload: IToggleColumnVisibilityAction['payload']
): IToggleColumnVisibilityAction => {
  return {
    type: toggleColumnVisibilityActionTypes.TOGGLE_SHOWN_COLUMN_ACTION_TYPES,
    payload,
  };
};
