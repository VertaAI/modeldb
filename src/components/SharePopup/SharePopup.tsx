import React, { CSSProperties } from 'react';
import ReactModal from 'react-modal';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { UserAccess } from '../../models/Project';
import User from '../../models/User';
import { resetInvitationState, sendInvitationForUser } from '../../store/collaboration';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { handleUserAuthentication } from '../../store/user';
import close from './images/close.svg';
import icon_check from './images/icon-check.svg';
import share_read_icon from './images/share-r-icon.svg';
import share_write_icon from './images/share-wr-icon.svg';
import styles from './SharePopup.module.css';

enum Tabs {
  collaborators = 0,
  share = 1
}

interface ILocalProps {
  showModal: boolean;
  projectName: string;
  collaborators: Map<User, UserAccess>;
  onRequestClose?(): void;
}

interface IPropsFromState {
  sending: boolean;
  result?: boolean | undefined;
}

interface ILocalState {
  activeTab: Tabs;
  emailValue: string;
  inputActive: boolean;
  showModal: boolean;
  userAccess: UserAccess;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class SharePopup extends React.Component<AllProps, ILocalState> {
  private shareInput?: HTMLInputElement;
  private initialState: ILocalState = {
    activeTab: this.props.collaborators.size > 1 ? Tabs.collaborators : Tabs.share,
    emailValue: '',
    inputActive: false,
    showModal: this.props.showModal,
    userAccess: UserAccess.Read
  };

  constructor(props: AllProps) {
    super(props);
    this.state = this.initialState;

    this.changeTab = this.changeTab.bind(this);
    this.selectShareTab = this.selectShareTab.bind(this);
    this.selectCollaboratorsTab = this.selectCollaboratorsTab.bind(this);
    this.handleCloseModal = this.handleCloseModal.bind(this);

    this.updateInputValue = this.updateInputValue.bind(this);
    this.changeShareType = this.changeShareType.bind(this);
    this.sendInvitationOnClick = this.sendInvitationOnClick.bind(this);
    this.sendNewInvitation = this.sendNewInvitation.bind(this);
  }

  public componentWillReceiveProps(nextProps: ILocalProps) {
    this.setState({ showModal: nextProps.showModal });
  }

  public render() {
    const { sending, result } = this.props;

    return (
      <ReactModal
        isOpen={this.state.showModal}
        contentLabel="sharePopup"
        onRequestClose={this.handleCloseModal}
        className={styles.modal_window}
        overlayClassName={styles.overlay}
      >
        <div className={styles.header}>
          <div className={styles.title}>{this.props.projectName}</div>
          <img src={close} className={styles.icon} onClick={this.handleCloseModal} />
        </div>
        <div className={styles.tabs}>
          <div className={styles.tabs_buttons}>
            <button
              className={`${styles.button_collaborators} ${this.state.activeTab === Tabs.collaborators ? styles.activeTab : ''}`}
              onClick={this.selectCollaboratorsTab}
            >
              Collaborators <span className={styles.collaborators_count}>{this.props.collaborators.size}</span>
            </button>
            <button
              className={`${styles.button_share} ${this.state.activeTab === Tabs.share ? styles.activeTab : ''}`}
              onClick={this.selectShareTab}
            >
              Share Project
            </button>
          </div>
        </div>
        <div className={styles.content}>
          {this.state.activeTab === Tabs.share ? (
            sending ? (
              ''
            ) : result === undefined ? (
              <div className={styles.content_share}>
                <div className={styles.content_header}>Invite People to the Project</div>
                <div>
                  <label className={`${styles.form_group} ${styles.content_label}`}>
                    <input
                      type="text"
                      placeholder=" "
                      className={styles.content_input}
                      value={this.state.emailValue}
                      onChange={this.updateInputValue}
                      ref={c => (this.shareInput = c!)}
                    />
                    <label className={`${styles.content_label} ${styles.content_placeholder}`}>Email or username</label>
                    <button className={styles.share_button} onClick={this.changeShareType}>
                      <img src={this.state.userAccess === UserAccess.Read ? share_read_icon : share_write_icon} />
                      <span className={styles.tooltiptext}>
                        Access type <br />
                        {this.state.userAccess === UserAccess.Read ? 'Read Only' : 'Read / Write'}
                      </span>
                    </button>
                  </label>
                </div>
                <div>
                  <button className={styles.send_invitation_button} onClick={this.sendInvitationOnClick}>
                    Send Invitation
                  </button>
                </div>
              </div>
            ) : result === true ? (
              <div className={styles.content_share_sucess}>
                <img src={icon_check} />
                <span className={styles.invitation_success_header}>Invitation to {this.state.emailValue} sent!</span>
                <button className={styles.invitation_success_button} onClick={this.sendNewInvitation}>
                  <span className={styles.invitation_success_button_text}>Send New Invitation</span>
                </button>
              </div>
            ) : (
              'Failure'
            )
          ) : this.state.activeTab === Tabs.collaborators ? (
            <div className={styles.content_collaborators}>Collaborators</div>
          ) : (
            ''
          )}
        </div>
      </ReactModal>
    );
  }

  private selectCollaboratorsTab() {
    this.changeTab(Tabs.collaborators);
  }

  private selectShareTab() {
    this.changeTab(Tabs.share);
  }

  private changeTab(tab: Tabs) {
    this.setState({ ...this.state, activeTab: tab });
  }

  private handleCloseModal() {
    if (this.props.onRequestClose) {
      this.props.onRequestClose();
      this.setState(this.initialState);
    }
  }

  private changeShareType() {
    switch (this.state.userAccess) {
      case UserAccess.Read:
        this.setState({ ...this.state, userAccess: UserAccess.Write });
        break;
      case UserAccess.Write:
        this.setState({ ...this.state, userAccess: UserAccess.Read });
        break;
    }
    this.shareInput!.focus();
  }

  private sendInvitationOnClick() {
    this.props.dispatch(sendInvitationForUser(this.state.emailValue, this.state.userAccess));
  }

  private sendNewInvitation() {
    this.props.dispatch(resetInvitationState());
    this.setState(this.initialState);
  }

  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value
    });
    event.preventDefault();
  }
}

const mapStateToProps = ({ collaboration }: IApplicationState) => ({
  result: collaboration.result,
  sending: collaboration.sending
});

export default connect(mapStateToProps)(SharePopup);
