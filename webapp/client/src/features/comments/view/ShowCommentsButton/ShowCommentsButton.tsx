import * as React from 'react';

import { ShowCommentsButton } from 'core/features/comments';

type AllProps = Omit<
  React.ComponentProps<typeof ShowCommentsButton>,
  'addCommentFormSettings' | 'commentSettings'
>;

class ShowCommentsButtonWithAuthorButton extends React.PureComponent<AllProps> {
  public render() {
    return (
      <ShowCommentsButton
        {...this.props as any}
        commentSettings={undefined}
        addCommentFormSettings={undefined}
      />
    );
  }
}

export default ShowCommentsButtonWithAuthorButton;
