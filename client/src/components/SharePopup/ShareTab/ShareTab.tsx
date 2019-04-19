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
import Preloader from 'components/shared/Preloader/Preloader';

import { selectInviteNewCollaboratorInfo } from 'store/collaboration/selectors';
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
  emailError: string | undefined;
  userAccess: UserAccess;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class ShareTab extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    emailValue: '',
    userAccess: UserAccess.Read,
    emailError: undefined,
  };

  public render() {
    const { status } = this.props;

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
            <div className={styles.userEmailInput}>
              <PlaceholderInput
                additionalClassName={styles.form_group}
                inputValue={this.state.emailValue}
                placeholderValue={'Email or username'}
                onInputChange={this.updateInputValue}
                onBlur={this.validateEmailValue}
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
              <span className={styles.userEmailInput_error}>
                {this.state.emailError}
              </span>
            </div>
            <Button
              disabled={this.state.emailError !== undefined}
              onClick={this.sendInvitationOnClick}
            >
              Send Invitation
            </Button>
          </div>
        );
      case InvitationStatus.Failure:
        return (
          <div className={styles.share_result_content}>
            <Icon type="error" className={styles.icon} />
            {/* todo fix it */}
            <span className={styles.share_result_header}>
              {'user not found'}
            </span>
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
        return (
          <div className={styles.share_result_content}>
            <Preloader variant="dots" />
          </div>
        );
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
      emailError: this.state.emailError
        ? validateEmailValue(event.target.value)
        : undefined,
    });
    event.preventDefault();
  }

  @bind
  private validateEmailValue() {
    this.setState({
      emailError: validateEmailValue(this.state.emailValue),
    });
  }

  @bind
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

// todo move this functions in utils folder

const validateEmailValue = combineValidators([validateNotEmpty, validateEmail]);

function combineValidators<T>(
  validators: Array<(value: T) => string | undefined>
): (value: T) => string | undefined {
  return (value: any) =>
    validators.reduce(
      (maybeError, validator) => {
        return maybeError !== undefined ? maybeError : validator(value);
      },
      undefined as string | undefined
    );
}

function validateNotEmpty(value: string) {
  return value === '' || value === null || value === undefined
    ? 'is empty!'
    : undefined;
}

function validateEmail(email: string) {
  var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
  return !re.test(String(email).toLowerCase()) ? 'invalid email!' : undefined;
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  const { error, status } = selectInviteNewCollaboratorInfo(state);
  return { error, status };
};

export default connect(mapStateToProps)(ShareTab);
