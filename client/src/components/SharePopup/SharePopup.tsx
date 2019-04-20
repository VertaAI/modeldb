import { bind } from 'decko';
import React from 'react';
import { connect } from 'react-redux';

import Popup from 'components/shared/Popup/Popup';
import Tabs from 'components/shared/Tabs/Tabs';
import { Project, UserAccess } from 'models/Project';
import User, { CurrentUser } from 'models/User';
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
  project: Project;
  onRequestClose?(): void;
}

interface IPropsFromState {
  currentUser: CurrentUser;
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
    activeTab: this.getActiveTabWhenShownPopup(this.props.project),
    showModal: this.props.showModal,
  };

  public componentDidUpdate(prevProps: AllProps) {
    if (!prevProps.showModal && this.props.showModal) {
      this.setState({
        activeTab: this.getActiveTabWhenShownPopup(this.props.project),
      });
    }
  }

  public render() {
    const {
      currentUser,
      project: { collaborators, name: projectName, id: projectId },
    } = this.props;
    let currentUserAccess = UserAccess.Read;
    Array.from(collaborators.entries()).forEach((value: [User, UserAccess]) => {
      const [user, userAccess] = value;

      if (user.id === currentUser.id) {
        currentUserAccess = userAccess;
      }
    });

    return (
      <Popup
        title={projectName}
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
            badge={collaborators.size}
          >
            <CollaboratorsTab
              currentUserAccess={currentUserAccess}
              projectId={projectId}
              collaborators={collaborators}
            />
          </Tabs.Tab>
          <Tabs.Tab title="Share Project" type={TabsType.share} centered={true}>
            <ShareTab
              currentUserAccess={currentUserAccess}
              projectId={projectId}
            />
          </Tabs.Tab>
        </Tabs>
      </Popup>
    );
  }

  private getActiveTabWhenShownPopup(project: Project) {
    return project.collaborators.size > 1
      ? TabsType.collaborators
      : TabsType.share;
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
        this.props.project.collaborators.size > 1
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
