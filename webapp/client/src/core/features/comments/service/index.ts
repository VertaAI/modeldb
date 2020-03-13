import { IComment } from '../Model';
import * as converters from './serverModel/Comments/converters';

import { ICommentsService as ICommentsService_ } from './CommentsService';
export type ICommentsService<Comment extends IComment> = ICommentsService_<
  Comment
>;
export { default as CommentsService } from './CommentsService';
export { converters };
