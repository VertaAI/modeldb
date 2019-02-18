import React from 'react';
import ReactModal from 'react-modal';
import { connect } from 'react-redux';
import { UserAccess } from '../../models/Project';
import User from '../../models/User';
import { InvitationStatus, resetInvitationState } from '../../store/collaboration';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import CollaboratorsTab from './CollaboratorsTab/CollaboratorsTab';
import close from './images/close.svg';
import styles from './SharePopup.module.css';
import ShareTab from './ShareTab/ShareTab';

enum Tabs {
  collaborators = 0,
  share = 1
}

interface ILocalProps {
  showModal: boolean;
  projectName: string;
  projectId: string;
  collaborators: Map<User, UserAccess>;
  onRequestClose?(): void;
}

interface IPropsFromState {
  status: InvitationStatus;
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

  constructor(props: AllProps) {
    super(props);
    this.state = { activeTab: this.props.collaborators.size > 1 ? Tabs.collaborators : Tabs.share, showModal: this.props.showModal };

    this.changeTab = this.changeTab.bind(this);
    this.selectShareTab = this.selectShareTab.bind(this);
    this.selectCollaboratorsTab = this.selectCollaboratorsTab.bind(this);
    this.handleCloseModal = this.handleCloseModal.bind(this);
  }

  public render() {
    const { status } = this.props;

    return (
      <ReactModal
        isOpen={this.state.showModal}
        contentLabel="sharePopup"
        onRequestClose={this.handleCloseModal}
        className={styles.modal_window}
        overlayClassName={styles.overlay}
        appElement={document.getElementById('root')!}
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
            <ShareTab status={status} projectId={this.props.projectId} />
          ) : this.state.activeTab === Tabs.collaborators ? (
            <CollaboratorsTab projectId={this.props.projectId} collaborators={this.props.collaborators} />
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
      this.props.dispatch(resetInvitationState());
      this.changeTab(this.props.collaborators.size > 1 ? Tabs.collaborators : Tabs.share);
    }
  }
}

const mapStateToProps = ({ collaboration }: IApplicationState) => ({
  status: collaboration.inviteNewCollaborator.status
});

export default connect(mapStateToProps)(SharePopup);
