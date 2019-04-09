import { bind } from 'decko';
import React from 'react';
import { connect } from 'react-redux';

import Button from 'components/shared/Button/Button';
import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import Icon from 'components/shared/Icon/Icon';
import { UserAccess } from 'models/Project';
import {
  InvitationStatus,
  resetInvitationState,
  sendInvitationForUser,
} from 'store/collaboration';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import { selectInviteNewCollaborator } from 'store/collaboration/selectors';
import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import { PlaceholderInput } from '../PlaceholderInput/PlaceholderInput';
import styles from './ShareTab.module.css';

interface ILocalProps {
  currentUserAccess: UserAccess;
  projectId: string;
}

interface IPropsFromState {
  error?: any;
  status: InvitationStatus;
}

interface ILocalState {
  emailValue: string;
  userAccess: UserAccess;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class ShareTab extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    emailValue: '',
    userAccess: UserAccess.Read,
  };

  public render() {
    const { status, error } = this.props;

    switch (status) {
      case InvitationStatus.None:
        return this.props.currentUserAccess === UserAccess.Read ? (
          <div className={styles.share_result_content}>
            <Icon type="read-only" className={styles.icon} />
            <span className={styles.share_result_header}>Read-only</span>
            <span className={styles.share_result_text}>
              You're restricted to share project
            </span>
          </div>
        ) : (
          <div className={styles.content_share}>
            <div className={styles.content_header}>
              Invite People to the Project
            </div>
            <div>
              <PlaceholderInput
                additionalClassName={styles.form_group}
                inputValue={this.state.emailValue}
                onInputChange={this.updateInputValue}
                placeholderValue={'Email or username'}
                additionalControl={
                  <ButtonTooltip
                    additionalClassName={styles.share_button}
                    icon={
                      this.state.userAccess === UserAccess.Read ? (
                        <Icon type="share-read" />
                      ) : (
                        <Icon type="share-write" />
                      )
                    }
                    toolTipContent={`Access type ${
                      this.state.userAccess === UserAccess.Read
                        ? 'Read Only'
                        : 'Read / Write'
                    }`}
                    onButtonClick={this.changeShareType}
                    width={93}
                  />
                }
              />
            </div>
            <Button onClick={this.sendInvitationOnClick}>
              Send Invitation
            </Button>
          </div>
        );
      case InvitationStatus.Failure:
        return (
          <div className={styles.share_result_content}>
            <Icon type="error" className={styles.icon} />
            <span className={styles.share_result_header}>{error}</span>
            <div>
              <ButtonLikeText onClick={this.trySendInvitationAgain}>
                Try Again
              </ButtonLikeText>{' '}
              <span className={styles.share_result_text}>or</span>{' '}
              <ButtonLikeText onClick={this.sendNewInvitation}>
                Send New Invitation
              </ButtonLikeText>
            </div>
          </div>
        );
      case InvitationStatus.Success:
        return (
          <div className={styles.share_result_content}>
            <Icon type="check" className={styles.icon} />
            <span className={styles.share_result_header}>
              Invitation to {this.state.emailValue} sent!
            </span>
            <ButtonLikeText onClick={this.sendNewInvitation}>
              Send New Invitation
            </ButtonLikeText>
          </div>
        );
      case InvitationStatus.Sending:
        return '';
      default:
        return '';
    }
  }

  @bind
  private changeShareType() {
    if (this.props.currentUserAccess === UserAccess.Read) {
      return;
    }
    switch (this.state.userAccess) {
      case UserAccess.Read:
        this.setState({ ...this.state, userAccess: UserAccess.Write });
        break;
      case UserAccess.Write:
        this.setState({ ...this.state, userAccess: UserAccess.Read });
        break;
    }
  }

  @bind
  private sendInvitationOnClick() {
    this.props.dispatch(
      sendInvitationForUser(
        this.props.projectId,
        this.state.emailValue,
        this.state.userAccess
      )
    );
  }

  @bind
  private sendNewInvitation() {
    this.props.dispatch(resetInvitationState());
    this.setState({ emailValue: '', userAccess: UserAccess.Read });
  }

  @bind
  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value,
    });
    event.preventDefault();
  }

  private trySendInvitationAgain() {
    this.props.dispatch(
      sendInvitationForUser(
        this.props.projectId,
        this.state.emailValue,
        this.state.userAccess
      )
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  const { error, status } = selectInviteNewCollaborator(state);
  return { error, status };
};

export default connect(mapStateToProps)(ShareTab);
