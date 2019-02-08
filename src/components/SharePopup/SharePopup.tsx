import React, { CSSProperties } from 'react';
import ReactModal from 'react-modal';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import User from '../../models/User';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import { handleUserAuthentication } from '../../store/user';
import close from './images/close.svg';
import share_read_icon from './images/share-r-icon.svg';
import styles from './SharePopup.module.css';

enum Tabs {
  collaborators = 0,
  share = 1
}

interface ILocalProps {
  showModal: boolean;
  projectName: string;
  collaborators?: User[];
  onRequestClose?(): void;
}

interface ILocalState {
  activeTab: Tabs;
  emailValue: string;
  inputActive: boolean;
  showModal: boolean;
}

class SharePopup extends React.Component<ILocalProps, ILocalState> {
  constructor(props: ILocalProps) {
    super(props);
    this.state = {
      activeTab: this.props.collaborators ? Tabs.collaborators : Tabs.share,
      emailValue: '',
      inputActive: false,
      showModal: this.props.showModal
    };

    this.changeTab = this.changeTab.bind(this);
    this.selectShareTab = this.selectShareTab.bind(this);
    this.selectCollaboratorsTab = this.selectCollaboratorsTab.bind(this);
    this.handleCloseModal = this.handleCloseModal.bind(this);

    this.updateInputValue = this.updateInputValue.bind(this);
    this.activateField = this.activateField.bind(this);
    this.disableFocus = this.disableFocus.bind(this);
  }

  public componentWillReceiveProps(nextProps: ILocalProps) {
    this.setState({ showModal: nextProps.showModal });
  }

  public render() {
    return (
      <ReactModal
        isOpen={this.state.showModal}
        contentLabel="sharePopup"
        onRequestClose={this.props.onRequestClose}
        className={styles.modal_window}
        overlayClassName={styles.overlay}
      >
        <div className={styles.header}>
          <div className={styles.title}>{this.props.projectName}</div>
          <img src={close} className={styles.icon} onClick={this.props.onRequestClose} />
        </div>
        <div className={styles.tabs}>
          <div className={styles.tabs_buttons}>
            <button
              className={`${styles.button_collaborators} ${this.state.activeTab === Tabs.collaborators ? styles.activeTab : ''}`}
              onClick={this.selectCollaboratorsTab}
            >
              Collaborators <span className={styles.collaborators_count}>8</span>
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
                  />
                  <label className={`${styles.content_label} ${styles.content_placeholder}`}>Email or username</label>
                  <button className={styles.share_button}>
                    <img src={share_read_icon} />
                    <span className={styles.tooltiptext}>
                      Access type <br />
                      Read Only
                    </span>
                  </button>
                </label>
              </div>
              <div>
                <button className={styles.send_invitation_button}>Send Invitation</button>
              </div>
            </div>
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
    }
  }

  private activateField() {
    this.setState({
      inputActive: false
    });
  }

  private disableFocus(event: React.ChangeEvent<HTMLInputElement>) {
    if (event.target.value === '') {
      this.setState({
        inputActive: false
      });
    }
  }

  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value
    });
    this.activateField();
    event.preventDefault();
  }
}

const mapStateToProps = () => ({});

export default connect(mapStateToProps)(SharePopup);
