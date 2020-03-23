import { createReducer, ActionType } from 'typesafe-actions';

import * as actions from './actions';
import { IWorkspaces } from './types';

export default createReducer<IWorkspaces, ActionType<typeof actions>>({
  data: {
    currentWorkspace: { type: 'user' },
  },
}) as any;
