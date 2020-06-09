import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { EntityId } from 'core/shared/models/Comment';
import {
  addComment,
  selectIsAddedComment,
  selectIsAddingComment,
  ICommentsRootState,
} from 'features/comments/store';
import { validateNotEmpty } from 'core/shared/utils/validators';
import Button from 'core/shared/view/elements/Button/Button';
import MuiTextInput from 'core/shared/view/elements/MuiTextInput/MuiTextInput';
import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';

import styles from './AddCommentForm.module.css';

interface ILocalProps {
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
    const { isAddingNewComment } = this.props;
    const { newMessage, newMessageError } = this.state;
    return (
      <div className={styles.root}>
        <form onSubmit={this.onAddComment}>
          <div className={styles.input}>
            <MuiTextInput
              value={newMessage}
              placeholder="Enter comments here."
              dataTest="comment-input"
              onChange={e => this.onChangeNewMessage(e.currentTarget.value)}
            />
            {newMessageError ? (
              <InlineErrorView error={newMessageError} />
            ) : null}
          </div>
          <div className={styles.addButton}>
            <Button
              isLoading={isAddingNewComment}
              disabled={Boolean(newMessageError)}
              type="submit"
              dataTest="send-comment-button"
            >
              Comment
            </Button>
          </div>
        </form>
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
  private onAddComment(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
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
