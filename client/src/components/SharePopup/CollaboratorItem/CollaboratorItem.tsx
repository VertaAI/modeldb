import { bind } from 'decko';
import React from 'react';
import Avatar from 'react-avatar';
import { connect } from 'react-redux';

import { UserAccess } from 'models/Project';
import User from 'models/User';
import {
  changeAccessToProject,
  removeAccessFromProject,
} from 'store/collaboration';
import { IConnectedReduxProps } from 'store/store';

import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import share_change_icon from '../images/share-change-icon.svg';
import share_delete_icon from '../images/share-del-icon.svg';
import share_read_icon from '../images/share-r-icon.svg';
import share_write_icon from '../images/share-wr-icon.svg';
import styles from './CollaboratorItem.module.css';

interface ILocalProps {
  currentUserAccess: UserAccess;
  user: User;
  userAccess: UserAccess;
  projectId: string;
  onChangeOwner(): void;
}

type AllProps = IConnectedReduxProps & ILocalProps;

class CollaboratorItem extends React.Component<AllProps> {
  public render() {
    const { currentUserAccess, user, userAccess } = this.props;

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
          {userAccess === UserAccess.Owner ? (
            <div className={styles.user_name_title}>Owner</div>
          ) : (
            ''
          )}
        </div>
        <div className={styles.collaborator_buttons}>
          <div>
            {currentUserAccess === UserAccess.Read ? (
              ''
            ) : userAccess === UserAccess.Read ? (
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${
                  styles.blue_button
                }`}
                imgSrc={share_read_icon}
                toolTipContent={'Read only'}
                onButtonClick={this.setWriteAccessToProject}
                width={79}
              />
            ) : userAccess === UserAccess.Write ? (
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${
                  styles.blue_button
                }`}
                imgSrc={share_write_icon}
                toolTipContent={'Read / Write'}
                onButtonClick={this.setReadAccessToProject}
                width={93}
              />
            ) : (
              ''
            )}
          </div>
          <div />
          <div>
            {userAccess === UserAccess.Owner &&
            currentUserAccess === UserAccess.Owner ? (
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${
                  styles.blue_button
                }`}
                imgSrc={share_change_icon}
                toolTipContent={'Change owner'}
                onButtonClick={this.props.onChangeOwner}
                width={104}
              />
            ) : currentUserAccess === UserAccess.Read ? (
              userAccess === UserAccess.Read ? (
                <ButtonTooltip
                  additionalClassName={`${styles.collaborator_button} ${
                    styles.without_active_button
                  }`}
                  imgSrc={share_read_icon}
                  toolTipContent={'Read only'}
                  width={79}
                />
              ) : userAccess === UserAccess.Write ? (
                <ButtonTooltip
                  additionalClassName={`${styles.collaborator_button} ${
                    styles.without_active_button
                  }`}
                  imgSrc={share_write_icon}
                  toolTipContent={'Read / Write'}
                  width={93}
                />
              ) : (
                ''
              )
            ) : userAccess === UserAccess.Read ||
              userAccess === UserAccess.Write ? (
              <ButtonTooltip
                additionalClassName={`${styles.collaborator_button} ${
                  styles.red_button
                }`}
                imgSrc={share_delete_icon}
                toolTipContent={'Delete'}
                onButtonClick={this.removeAccessToProject}
                width={59}
              />
            ) : (
              ''
            )}
          </div>
        </div>
      </div>
    );
  }

  @bind
  private setWriteAccessToProject() {
    this.props.dispatch(
      changeAccessToProject(
        this.props.projectId,
        this.props.user,
        UserAccess.Write
      )
    );
  }

  @bind
  private setReadAccessToProject() {
    this.props.dispatch(
      changeAccessToProject(
        this.props.projectId,
        this.props.user,
        UserAccess.Read
      )
    );
  }

  @bind
  private removeAccessToProject() {
    this.props.dispatch(
      removeAccessFromProject(this.props.projectId, this.props.user)
    );
  }
}

const mapStateToProps = () => ({});

export default connect(mapStateToProps)(CollaboratorItem);
