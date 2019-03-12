import React from 'react';
import { connect } from 'react-redux';
import { UserAccess } from '../../../models/Project';
import User from '../../../models/User';
import { InvitationStatus, resetInvitationState, sendInvitationForUser } from '../../../store/collaboration';
import { IApplicationState, IConnectedReduxProps } from '../../../store/store';
import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import error_icon from '../images/error-icon.svg';
import icon_check from '../images/icon-check.svg';
import read_only_icon from '../images/read-only-icon.svg';
import share_read_icon from '../images/share-r-icon.svg';
import share_write_icon from '../images/share-wr-icon.svg';
import { PlaceholderInput } from '../PlaceholderInput/PlaceholderInput';
import styles from './ShareTab.module.css';

interface ILocalProps {
  currentUserAccess: UserAccess;
  // should be changed to actual type after getting format from the backend
  error?: any;
  status: InvitationStatus;
  projectId: string;
}

interface ILocalState {
  emailValue: string;
  userAccess: UserAccess;
}

type AllProps = IConnectedReduxProps & ILocalProps;

class ShareTab extends React.Component<AllProps, ILocalState> {
  constructor(props: AllProps) {
    super(props);

    this.state = {
      emailValue: '',
      userAccess: UserAccess.Read
    };

    this.sendInvitationOnClick = this.sendInvitationOnClick.bind(this);
    this.changeShareType = this.changeShareType.bind(this);
    this.updateInputValue = this.updateInputValue.bind(this);
    this.sendNewInvitation = this.sendNewInvitation.bind(this);
    this.trySendInvitationAgain = this.trySendInvitationAgain.bind(this);
  }

  public render() {
    const { status, error } = this.props;

    switch (status) {
      case InvitationStatus.None:
        return this.props.currentUserAccess !== UserAccess.Read ? (
          <div className={styles.share_result_content}>
            <img src={read_only_icon} />
            <span className={styles.share_result_header}>Read-only</span>
            <span className={styles.share_result_text}>You're restricted to share project</span>
          </div>
        ) : (
          <div className={styles.content_share}>
            <div className={styles.content_header}>Invite People to the Project</div>
            <div>
              <PlaceholderInput
                additionalClassName={styles.form_group}
                inputValue={this.state.emailValue}
                onInputChange={this.updateInputValue}
                placeholderValue={'Email or username'}
                additionalControl={
                  <ButtonTooltip
                    additionalClassName={styles.share_button}
                    imgSrc={this.state.userAccess === UserAccess.Read ? share_read_icon : share_write_icon}
                    toolTipContent={`Access type ${this.state.userAccess === UserAccess.Read ? 'Read Only' : 'Read / Write'}`}
                    onButtonClick={this.changeShareType}
                    width={93}
                  />
                }
              />
            </div>
            <div>
              <button className={styles.send_invitation_button} onClick={this.sendInvitationOnClick}>
                Send Invitation
              </button>
            </div>
          </div>
        );
      case InvitationStatus.Failure:
        return (
          <div className={styles.share_result_content}>
            <img src={error_icon} />
            <span className={styles.share_result_header}>{error}</span>
            <div>
              <button className={styles.share_result_button} onClick={this.trySendInvitationAgain}>
                <span className={`${styles.share_result_button_text} ${styles.share_result_text}`}>Try Again</span>
              </button>
              <span className={styles.share_result_text}>or</span>
              <button className={styles.share_result_button} onClick={this.sendNewInvitation}>
                <span className={`${styles.share_result_button_text} ${styles.share_result_text}`}>Send New Invitation</span>
              </button>
            </div>
          </div>
        );
      case InvitationStatus.Success:
        return (
          <div className={styles.share_result_content}>
            <img src={icon_check} />
            <span className={styles.share_result_header}>Invitation to {this.state.emailValue} sent!</span>
            <button className={styles.share_result_button} onClick={this.sendNewInvitation}>
              <span className={`${styles.share_result_button_text} ${styles.share_result_text}`}>Send New Invitation</span>
            </button>
          </div>
        );
      case InvitationStatus.Sending:
        return '';
      default:
        return '';
    }
  }

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

  private sendInvitationOnClick() {
    this.props.dispatch(sendInvitationForUser(this.props.projectId, this.state.emailValue, this.state.userAccess));
  }

  private sendNewInvitation() {
    this.props.dispatch(resetInvitationState());
    this.setState({ emailValue: '', userAccess: UserAccess.Read });
  }

  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value
    });
    event.preventDefault();
  }

  private trySendInvitationAgain() {
    this.props.dispatch(sendInvitationForUser(this.props.projectId, this.state.emailValue, this.state.userAccess));
  }
}

const mapStateToProps = ({ collaboration }: IApplicationState) => ({
  error: collaboration.inviteNewCollaborator.error,
  status: collaboration.inviteNewCollaborator.status
});

export default connect(mapStateToProps)(ShareTab);
