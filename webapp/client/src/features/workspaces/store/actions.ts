import { ActionResult } from 'setup/store/store';
import { action } from 'typesafe-actions';

export const loadWorkspaces = (): ActionResult<void, any> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action('@@workspaces/LOAD_WORKSPACES'));

  dispatch(action('@@workspaces/LOAD_WORKSPACES_SUCCESS'));
};
