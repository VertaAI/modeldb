import cn from 'classnames';
import { bind } from 'decko';
import moment from 'moment';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { EntityId, IComment } from 'core/features/comments/Model';
import {
  deleteComment,
  selectIsDeletingComment,
  ICommentsRootState,
} from 'core/features/comments/store';
import ButtonLikeText from 'core/shared/view/elements/ButtonLikeText/ButtonLikeText';

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
      <div className={styles.root} data-test="comment">
        <div
          className={cn(styles.root, { [styles.deleting]: isDeleting })}
          key={data.id}
        >
          {commentSettings && (
            <div className={styles.leftContent}>
              {commentSettings.renderAuthorAvatar({ comment: data })}
            </div>
          )}
          <div className={styles.rightContent}>
            <div className={styles.header}>
              {commentSettings && (
                <span className={styles.username} data-test="comment-author">
                  {commentSettings.renderAuthorName({ comment: data })}
                </span>
              )}
              <span className={styles.time}>
                {moment(data.dateTime).fromNow()}
              </span>
            </div>
            <div className={styles.message} data-test="comment-message">
              {data.message}
            </div>
            <div className={styles.actions}>
              <div className={styles.action}>
                {canCurrentUserDeleteComment({ comment: data }) ? (
                  <ButtonLikeText
                    isDisabled={isDeleting}
                    dataTest="delete-comment-button"
                    onClick={this.makeOnDeleteComment(data.id)}
                  >
                    delete
                  </ButtonLikeText>
                ) : null}
              </div>
            </div>
          </div>
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
