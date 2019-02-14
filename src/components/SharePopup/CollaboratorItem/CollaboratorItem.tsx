import React from 'react';
import Avatar from 'react-avatar';
import { connect } from 'react-redux';
import { UserAccess } from '../../../models/Project';
import User from '../../../models/User';
import { changeAccessToProject, removeAccessFromProject } from '../../../store/collaboration';
import { IApplicationState, IConnectedReduxProps } from '../../../store/store';
import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import share_change_icon from '../images/share-change-icon.svg';
import share_delete_icon from '../images/share-del-icon.svg';
import share_read_icon from '../images/share-r-icon.svg';
import share_write_icon from '../images/share-wr-icon.svg';
import styles from './CollaboratorItem.module.css';

interface ILocalProps {
  user: User;
  userAccess: UserAccess;
  projectId: string;
  onChangeOwner(): void;
}

interface IPropsFromState {
  currentUser: User;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class CollaboratorItem extends React.Component<AllProps> {
  constructor(props: AllProps) {
    super(props);

    this.setWriteAccessToProject = this.setWriteAccessToProject.bind(this);
    this.setReadAccessToProject = this.setReadAccessToProject.bind(this);
    this.removeAccessToProject = this.removeAccessToProject.bind(this);
  }

  public render() {
    const { user, userAccess } = this.props;
    return (
      <div className={styles.collaborator}>
        <Avatar
          name={user.getNameOrEmail()}
          round={true}
          size="40"
          textSizeRatio={40 / 16}
          className={styles.user_avatar}
          src={user.picture ? user.picture : ''}
        />
        <div className={styles.user_name}>
          <div>{user.getNameOrEmail()}</div>
          {userAccess === UserAccess.Owner ? <div className={styles.user_name_title}>Owner</div> : ''}
        </div>
        <div className={styles.user_access}>
          {userAccess === UserAccess.Owner ? (
            <div className={styles.collaborator_buttons}>
              <div />
              <div />
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${styles.blue_button}`}
                imgSrc={share_change_icon}
                toolTipContent={'Change owner'}
                onButtonClick={this.props.onChangeOwner}
                width={104}
              />
            </div>
          ) : userAccess === UserAccess.Read ? (
            <div className={styles.collaborator_buttons}>
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${styles.blue_button}`}
                imgSrc={share_read_icon}
                toolTipContent={'Read only'}
                onButtonClick={this.setWriteAccessToProject}
                width={79}
              />
              <div />
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${styles.red_button}`}
                imgSrc={share_delete_icon}
                toolTipContent={'Delete'}
                onButtonClick={this.removeAccessToProject}
                width={59}
              />
            </div>
          ) : userAccess === UserAccess.Write ? (
            <div className={styles.collaborator_buttons}>
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${styles.blue_button}`}
                imgSrc={share_write_icon}
                toolTipContent={'Read / Write'}
                onButtonClick={this.setReadAccessToProject}
                width={93}
              />
              <div />
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${styles.red_button}`}
                imgSrc={share_delete_icon}
                toolTipContent={'Delete'}
                onButtonClick={this.removeAccessToProject}
                width={59}
              />
            </div>
          ) : (
            ''
          )}
        </div>
      </div>
    );
  }

  private setWriteAccessToProject() {
    this.props.dispatch(changeAccessToProject(this.props.projectId, this.props.user, UserAccess.Write));
  }

  private setReadAccessToProject() {
    this.props.dispatch(changeAccessToProject(this.props.projectId, this.props.user, UserAccess.Read));
  }

  private removeAccessToProject() {
    this.props.dispatch(removeAccessFromProject(this.props.projectId, this.props.user));
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  currentUser: layout.user!
});

export default connect(mapStateToProps)(CollaboratorItem);
