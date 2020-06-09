import { IComment } from '../../../../core/shared/models/Comment';

export interface IRequiredEntityInfo {
  id: string;
  name: string;
}

export interface IWithCommentSettings<Comment extends IComment = IComment> {
  commentSettings:
    | {
        renderAuthorAvatar(props: { comment: Comment }): React.ReactNode;
        renderAuthorName(props: { comment: Comment }): React.ReactNode;
        canCurrentUserDeleteComment(props: {
          comment: Comment;
        }): React.ReactNode;
      }
    | undefined;
}

export interface IWithAddCommentFormSettings {
  addCommentFormSettings:
    | {
        renderCurrentUserAvatar(): React.ReactNode;
      }
    | undefined;
}
