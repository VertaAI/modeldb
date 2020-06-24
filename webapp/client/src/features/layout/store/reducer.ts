import { createReducer, ActionType } from 'typesafe-actions';

import * as actions from './actions';
import { ILayoutState } from './types';

export default createReducer<ILayoutState, ActionType<typeof actions>>({
  isCollapsedSidebar: false,
}).handleAction(actions.toggleCollapsingSidebar, state => ({
  ...state,
  isCollapsedSidebar: !state.isCollapsedSidebar,
}));
