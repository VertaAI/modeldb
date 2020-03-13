import {
  ICommentsService,
  commentsReducer,
  ICommentsRootState as ICommentsRootState_,
  Deps,
  setEntitiesComments,
  ISetEntitiesComments,
} from 'core/features/comments';
import { IApplicationState } from 'store/store';

import * as Model from './Model';
import { CommentsService } from './service';

export type ISetEntitiesCommentsWithAuthor = ISetEntitiesComments<
  Model.IComment
>;
export const setEntitiesCommentsWithAuthor = (
  payload: ISetEntitiesCommentsWithAuthor['payload']
) => setEntitiesComments(payload);

export const makeCommentsService: Deps.IMakeCommentsService<
  IApplicationState,
  Model.IComment
> = () => {
  const commentsService = new CommentsService();
  return commentsService;
};

export { CommentsService, convertServerComment } from './service';
export { ShowCommentsButton } from './view';

export type ICommentsRootState = ICommentsRootState_;
export * from 'core/features/comments/store/deps';

export { Model };

export { commentsReducer };
