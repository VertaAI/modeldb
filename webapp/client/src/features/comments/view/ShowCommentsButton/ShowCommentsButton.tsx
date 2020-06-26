import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { IComment } from 'shared/models/Comment';
import {
  loadComments,
  selectEntityComments,
  ICommentsRootState,
} from 'features/comments/store';
import FaiWithLabel from 'shared/view/elements/FaiWithLabel/FaiWithLabel';

import OverflowingComments from './OverflowingComments/OverflowingComments';
import styles from './ShowCommentsButton.module.css';
import {
  IRequiredEntityInfo,
  IWithAddCommentFormSettings,
  IWithCommentSettings,
} from './types';
import Badge from './shared/Badge/Badge';

type ILocalProps = (
  | {
      buttonType: 'faiWithLabel';
      entityInfo: IRequiredEntityInfo;
      onHover(): void;
      onUnhover(): void;
    }
  | {
      buttonType: 'badge';
      entityInfo: IRequiredEntityInfo;
    }) &
  IWithAddCommentFormSettings &
  IWithCommentSettings;

interface IPropsFromState {
  comments: IComment[] | undefined;
}

interface IActionProps {
  loadComments: typeof loadComments;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

interface ILocalState {
  isShowComments: boolean;
}

class ShowCommentsButton<Comment extends IComment> extends React.PureComponent<
  AllProps,
  ILocalState
> {
  public state: ILocalState = {
    isShowComments: false,
  };

  public render() {
    return (
      <div className={styles.root}>
        {this.props.buttonType === 'badge' ? (
          <div className={styles.badge} onClick={this.showComments}>
            <Badge count={(this.props.comments || []).length} />
          </div>
        ) : (
          <>
            <FaiWithLabel
              theme="blue"
              iconType={'comment'}
              label={'show comments'}
              dataTest="show-comments-button"
              onClick={this.showComments}
              onHover={this.props.onHover}
              onUnhover={this.props.onUnhover}
            />
            <div className={styles.miniCommentNumber}>
              {this.props.comments && (
                <span data-test="show-comments-button-badge">
                  {this.props.comments.length}
                </span>
              )}
            </div>
          </>
        )}
        {this.state.isShowComments && (
          <OverflowingComments
            entityInfo={this.props.entityInfo}
            addCommentFormSettings={this.props.addCommentFormSettings}
            commentSettings={this.props.commentSettings}
            onClose={this.closeComments}
          />
        )}
      </div>
    );
  }

  @bind
  private showComments() {
    this.setState({ isShowComments: true });
  }
  @bind
  private closeComments() {
    this.setState({ isShowComments: false });
  }
}

const mapStateToProps = (
  state: ICommentsRootState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    comments: selectEntityComments(state, localProps.entityInfo.id),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators(
    {
      loadComments,
    },
    dispatch
  );

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ShowCommentsButton);
