import { bind } from 'decko';
import React from 'react';
import { connect } from 'react-redux';

import Popup from 'components/shared/Popup/Popup';
import Tabs from 'components/shared/Tabs/Tabs';
import { UserAccess } from 'models/Project';
import User from 'models/User';
import { resetInvitationState } from 'store/collaboration';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { selectCurrentUser } from 'store/user';

import CollaboratorsTab from './CollaboratorsTab/CollaboratorsTab';
import ShareTab from './ShareTab/ShareTab';

enum TabsType {
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
  activeTab: TabsType;
  showModal: boolean;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class SharePopup extends React.Component<AllProps, ILocalState> {
  public static getDerivedStateFromProps(nextProps: AllProps) {
    return { showModal: nextProps.showModal };
  }
  public state: ILocalState = {
    activeTab:
      this.props.collaborators.size > 1
        ? TabsType.collaborators
        : TabsType.share,
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
        <Tabs<TabsType>
          active={this.state.activeTab}
          onSelectTab={this.changeTab}
        >
          <Tabs.Tab
            title="Collaborators"
            type={TabsType.collaborators}
            badge={this.props.collaborators.size}
          >
            <CollaboratorsTab
              currentUserAccess={currentUserAccess}
              projectId={this.props.projectId}
              collaborators={this.props.collaborators}
            />
          </Tabs.Tab>
          <Tabs.Tab title="Share Project" type={TabsType.share} centered={true}>
            <ShareTab
              currentUserAccess={currentUserAccess}
              projectId={this.props.projectId}
            />
          </Tabs.Tab>
        </Tabs>
      </Popup>
    );
  }

  @bind
  private changeTab(tab: TabsType) {
    this.setState({ ...this.state, activeTab: tab });
  }

  @bind
  private handleCloseModal() {
    if (this.props.onRequestClose) {
      this.props.onRequestClose();
      this.props.dispatch(resetInvitationState());
      this.changeTab(
        this.props.collaborators.size > 1
          ? TabsType.collaborators
          : TabsType.share
      );
    }
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  currentUser: selectCurrentUser(state)!,
});

export default connect(mapStateToProps)(SharePopup);
