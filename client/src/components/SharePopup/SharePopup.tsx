import { bind } from 'decko';
import React from 'react';
import { connect } from 'react-redux';

import Popup from 'components/shared/Popup/Popup';
import { UserAccess } from 'models/Project';
import User from 'models/User';
import { resetInvitationState } from 'store/collaboration';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { selectCurrentUser } from 'store/user';

import CollaboratorsTab from './CollaboratorsTab/CollaboratorsTab';
import close from './images/close.svg';
import styles from './SharePopup.module.css';
import ShareTab from './ShareTab/ShareTab';

enum Tabs {
  collaborators = 0,
  share = 1,
}

interface ILocalProps {
  showModal: boolean;
  projectName: string;
  projectId: string;
  collaborators: Map<User, UserAccess>;
  onRequestClose?(): void;
}

interface IPropsFromState {
  currentUser: User;
}

interface ILocalState {
  activeTab: Tabs;
  showModal: boolean;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class SharePopup extends React.Component<AllProps, ILocalState> {
  public static getDerivedStateFromProps(nextProps: AllProps) {
    return { showModal: nextProps.showModal };
  }
  public state: ILocalState = {
    activeTab:
      this.props.collaborators.size > 1 ? Tabs.collaborators : Tabs.share,
    showModal: this.props.showModal,
  };

  public render() {
    let currentUserAccess = UserAccess.Read;
    Array.from(this.props.collaborators.entries()).forEach(
      (value: [User, UserAccess]) => {
        const [user, userAccess] = value;

        if (user.email === this.props.currentUser.email) {
          currentUserAccess = userAccess;
        }
      }
    );

    return (
      <Popup
        title={this.props.projectName}
        contentLabel="sharePopup"
        isOpen={this.state.showModal}
        onRequestClose={this.handleCloseModal}
      >
        <div className={styles.tabs}>
          <div className={styles.tabs_buttons}>
            <button
              className={`${styles.button_collaborators} ${
                this.state.activeTab === Tabs.collaborators
                  ? styles.activeTab
                  : ''
              }`}
              onClick={this.selectCollaboratorsTab}
            >
              Collaborators{' '}
              <span className={styles.collaborators_count}>
                {this.props.collaborators.size}
              </span>
            </button>
            <button
              className={`${styles.button_share} ${
                this.state.activeTab === Tabs.share ? styles.activeTab : ''
              }`}
              onClick={this.selectShareTab}
            >
              Share Project
            </button>
          </div>
        </div>
        <div className={styles.content}>
          {this.state.activeTab === Tabs.share ? (
            <ShareTab
              currentUserAccess={currentUserAccess}
              projectId={this.props.projectId}
            />
          ) : this.state.activeTab === Tabs.collaborators ? (
            <CollaboratorsTab
              currentUserAccess={currentUserAccess}
              projectId={this.props.projectId}
              collaborators={this.props.collaborators}
            />
          ) : (
            ''
          )}
        </div>
      </Popup>
    );
  }

  @bind
  private selectCollaboratorsTab() {
    this.changeTab(Tabs.collaborators);
  }

  @bind
  private selectShareTab() {
    this.changeTab(Tabs.share);
  }

  @bind
  private changeTab(tab: Tabs) {
    this.setState({ ...this.state, activeTab: tab });
  }

  @bind
  private handleCloseModal() {
    if (this.props.onRequestClose) {
      this.props.onRequestClose();
      this.props.dispatch(resetInvitationState());
      this.changeTab(
        this.props.collaborators.size > 1 ? Tabs.collaborators : Tabs.share
      );
    }
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  currentUser: selectCurrentUser(state)!,
});

export default connect(mapStateToProps)(SharePopup);
