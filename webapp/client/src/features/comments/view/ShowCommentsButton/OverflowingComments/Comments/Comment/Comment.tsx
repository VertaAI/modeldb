import cn from 'classnames';
import { bind } from 'decko';
import moment from 'moment';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { EntityId, IComment } from 'core/shared/models/Comment';
import {
  deleteComment,
  selectIsDeletingComment,
  ICommentsRootState,
} from 'features/comments/store';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import { IWithCommentSettings } from '../../../types';
import styles from './Comment.module.css';

interface ILocalProps extends IWithCommentSettings {
  entityId: EntityId;
  data: IComment;
}

interface IPropsFromState {
  isDeleting: boolean;
}

interface IActionProps {
  deleteComment: typeof deleteComment;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class Comment extends React.PureComponent<AllProps> {
  public render() {
    const { data, isDeleting, commentSettings } = this.props;

    const canCurrentUserDeleteComment = commentSettings
      ? commentSettings.canCurrentUserDeleteComment
      : () => true;

    return (
      <div
        className={cn(styles.root, {
          [styles.deleting]: isDeleting,
          [styles.currentUser]: canCurrentUserDeleteComment({ comment: data }),
        })}
        data-test="comment"
      >
        <div className={styles.header}>
          {commentSettings && (
            <div className={styles.avatar}>
              {commentSettings.renderAuthorAvatar({ comment: data })}
            </div>
          )}
          {commentSettings && (
            <span className={styles.username} data-test="comment-author">
              {commentSettings.renderAuthorName({ comment: data })}
            </span>
          )}
          <span className={styles.time}>{moment(data.dateTime).fromNow()}</span>
          {canCurrentUserDeleteComment({ comment: data }) && (
            <Icon
              className={styles.delete}
              type="trash"
              onClick={this.makeOnDeleteComment(data.id)}
            />
          )}
        </div>
        <div className={styles.message} data-test="comment-message">
          {data.message}
        </div>
      </div>
    );
  }

  @bind
  private makeOnDeleteComment(commentId: string) {
    return () => {
      if (!this.props.isDeleting) {
        this.props.deleteComment(this.props.entityId, commentId);
      }
    };
  }
}

const mapStateToProps = (
  state: ICommentsRootState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    isDeleting: selectIsDeletingComment(state, localProps.data.id),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators(
    {
      deleteComment,
    },
    dispatch
  );

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Comment);
