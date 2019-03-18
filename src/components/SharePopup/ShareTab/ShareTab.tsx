import React from 'react';
import { connect } from 'react-redux';
import { bind } from 'decko';
import { UserAccess } from '../../../models/Project';
import User from '../../../models/User';
import { InvitationStatus, resetInvitationState, sendInvitationForUser } from '../../../store/collaboration';
import { IApplicationState, IConnectedReduxProps } from '../../../store/store';
import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import icon_check from '../images/icon-check.svg';
import share_read_icon from '../images/share-r-icon.svg';
import share_write_icon from '../images/share-wr-icon.svg';
import { PlaceholderInput } from '../PlaceholderInput/PlaceholderInput';
import styles from './ShareTab.module.css';

interface ILocalProps {
  currentUserAccess: UserAccess;
  status: InvitationStatus;
  projectId: string;
}

interface ILocalState {
  emailValue: string;
  userAccess: UserAccess;
}

type AllProps = IConnectedReduxProps & ILocalProps;

class ShareTab extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    emailValue: '',
    userAccess: UserAccess.Read
  };

  public render() {
    const { status } = this.props;

    switch (status) {
      case InvitationStatus.None:
        return (
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
      case InvitationStatus.Success:
        return (
          <div className={styles.content_share_sucess}>
            <img src={icon_check} />
            <span className={styles.invitation_success_header}>Invitation to {this.state.emailValue} sent!</span>
            <button className={styles.invitation_success_button} onClick={this.sendNewInvitation}>
              <span className={styles.invitation_success_button_text}>Send New Invitation</span>
            </button>
          </div>
        );
      case InvitationStatus.Failure:
        return '';
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
    this.props.dispatch(sendInvitationForUser(this.props.projectId, this.state.emailValue, this.state.userAccess));
  }

  @bind
  private sendNewInvitation() {
    this.props.dispatch(resetInvitationState());
    this.setState({ emailValue: '', userAccess: UserAccess.Read });
  }

  @bind
  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value
    });
    event.preventDefault();
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  currentUser: layout.user!
});

export default connect(mapStateToProps)(ShareTab);
