import { Dispatch, Action, AnyAction } from 'redux';
import { ThunkAction } from 'redux-thunk';

import { IComment } from '../Model';
import { ICommentsService } from '../service';
import { ICommentsRootState } from './types';

export type IMakeCommentsService<
  State extends ICommentsRootState,
  Comment extends IComment
> = (settings: {
  dispatch: Dispatch;
  getState: () => State;
}) => ICommentsService<Comment>;

export interface IThunkActionDependencies<
  State extends ICommentsRootState = ICommentsRootState,
  Comment extends IComment = IComment
> {
  makeCommentsService: IMakeCommentsService<State, Comment>;
}

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<
  R,
  ICommentsRootState<IComment>,
  IThunkActionDependencies,
  A
>;
