import { IComment } from '../../../Model';

export const convertServerComment = <Comment extends IComment>(
  serverComment: any
): Comment => {
  return {
    id: serverComment.id,
    dateTime: new Date(Number(serverComment.date_time)),
    message: serverComment.message,
  } as Comment;
};
