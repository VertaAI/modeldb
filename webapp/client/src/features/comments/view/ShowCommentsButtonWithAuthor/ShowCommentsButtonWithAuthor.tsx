import * as React from 'react';

import {
  ShowCommentsButton,
  IWithAddCommentFormSettings,
  IWithCommentSettings,
} from 'features/comments';
import { IComment } from 'shared/models/Comment';
import { unknownUser } from 'shared/models/User';

import CommentUserAvatar from './CommentUserAvatar/CommentUserAvatar';

type AllProps = Omit<
  React.ComponentProps<typeof ShowCommentsButton>,
  'addCommentFormSettings' | 'commentSettings'
>;

class ShowCommentsButtonWithAuthor extends React.PureComponent<AllProps> {
  private commentSettings: IWithCommentSettings<IComment>['commentSettings'] = {
    renderAuthorName: () => unknownUser.username,
    canCurrentUserDeleteComment: () => true,
    renderAuthorAvatar: () => <CommentUserAvatar user={unknownUser} />,
  };

  private addCommentFormSettings: IWithAddCommentFormSettings['addCommentFormSettings'] = {
    renderCurrentUserAvatar: () => <CommentUserAvatar user={unknownUser} />,
  };

  public render() {
    return (
      <ShowCommentsButton
        {...this.props as any}
        commentSettings={this.commentSettings}
        addCommentFormSettings={this.addCommentFormSettings}
      />
    );
  }
}

export default ShowCommentsButtonWithAuthor;
