import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { EntityId } from 'core/features/comments/Model';
import {
  addComment,
  selectIsAddedComment,
  selectIsAddingComment,
  ICommentsRootState,
} from 'core/features/comments/store';
import { validateNotEmpty } from 'core/shared/utils/validators';
import Button from 'core/shared/view/elements/Button/Button';
import TextInput from 'core/shared/view/elements/TextInput/TextInput';

import { IWithAddCommentFormSettings } from '../../../types';
import styles from './AddCommentForm.module.css';

interface ILocalProps extends IWithAddCommentFormSettings {
  entityId: EntityId;
}

interface IPropsFromState {
  isAddedNewComment: boolean;
  isAddingNewComment: boolean;
}

interface IActionProps {
  addComment: typeof addComment;
}

interface ILocalState {
  newMessage: string;
  newMessageError: string | undefined;
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators(
    {
      addComment,
    },
    dispatch
  );

const mapStateToProps = (state: ICommentsRootState): IPropsFromState => {
  return {
    isAddedNewComment: selectIsAddedComment(state),
    isAddingNewComment: selectIsAddingComment(state),
  };
};

type AllProps = ILocalProps &
  ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

const initialState: ILocalState = {
  newMessage: '',
  newMessageError: undefined,
};

const validateMessage = validateNotEmpty('message');

class AddCommentForm extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = initialState;

  public componentDidUpdate(prevProps: AllProps) {
    if (!prevProps.isAddedNewComment && this.props.isAddedNewComment) {
      this.setState(initialState);
    }
  }

  public render() {
    const { addCommentFormSettings, isAddingNewComment } = this.props;
    const { newMessage, newMessageError } = this.state;
    return (
      <div className={styles.root}>
        {addCommentFormSettings && (
          <div className={styles.leftContent}>
            {addCommentFormSettings.renderCurrentUserAvatar()}
          </div>
        )}
        <div className={styles.rightContent}>
          <div className={styles.input}>
            <TextInput
              value={newMessage}
              size="small"
              dataTest="comment-input"
              onChange={this.onChangeNewMessage}
            />
            {newMessageError ? (
              <div className={styles.input_error}>{newMessageError}</div>
            ) : null}
          </div>
          <div className={styles.addButton}>
            <Button
              fullWidth={true}
              isLoading={isAddingNewComment}
              disabled={Boolean(newMessageError)}
              dataTest="send-comment-button"
              onClick={this.onAddComment}
            >
              Comment
            </Button>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private onChangeNewMessage(value: string) {
    this.setState({
      newMessage: value,
      newMessageError: this.state.newMessageError
        ? validateMessage(value)
        : undefined,
    });
  }

  @bind
  private onAddComment() {
    const newMessageError = validateMessage(this.state.newMessage);
    if (newMessageError) {
      this.setState({ newMessageError });
    } else {
      this.props.addComment(this.props.entityId, this.state.newMessage);
    }
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AddCommentForm);
