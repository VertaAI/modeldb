import * as React from 'react';
import Avatar from 'react-avatar';

import User from 'core/shared/models/User';

interface ILocalProps {
  user: User;
}

class CommentUserAvatar extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <Avatar
        name={this.props.user.username}
        round={true}
        size="30"
        textSizeRatio={36 / 16}
        src={this.props.user.picture || ''}
      />
    );
  }
}

export default CommentUserAvatar;
