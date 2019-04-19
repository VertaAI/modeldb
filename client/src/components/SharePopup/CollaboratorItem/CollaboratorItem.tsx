import { bind } from 'decko';
import React from 'react';
import Avatar from 'react-avatar';
import { connect } from 'react-redux';

import Icon from 'components/shared/Icon/Icon';
import Preloader from 'components/shared/Preloader/Preloader';
import { UserAccess } from 'models/Project';
import User from 'models/User';
import {
  changeAccessToProject,
  removeAccessFromProject,
  selectIsChangingUserAccess,
  selectIsRemovingUserAccess,
} from 'store/collaboration';
import { IConnectedReduxProps, IApplicationState } from 'store/store';

import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import styles from './CollaboratorItem.module.css';

interface ILocalProps {
  currentUserAccess: UserAccess;
  user: User;
  userAccess: UserAccess;
  projectId: string;
  onChangeOwner(): void;
}

interface IPropsFromState {
  isChangingAccess: boolean;
  isRemovingAccess: boolean;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class CollaboratorItem extends React.Component<AllProps> {
  public render() {
    const {
      currentUserAccess,
      isChangingAccess,
      isRemovingAccess,
      user,
      userAccess,
    } = this.props;

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
          {(() => {
            if (isChangingAccess || isRemovingAccess) {
              return <Preloader variant="dots" />;
            }
            return (
              <>
                <div>
                  {currentUserAccess === UserAccess.Read ? (
                    ''
                  ) : userAccess === UserAccess.Read ? (
                    <ButtonTooltip
                      additionalClassName={`${styles.collaborator_button} ${
                        styles.blue_button
                      }`}
                      icon={<Icon type="share-read" />}
                      toolTipContent={'Read only'}
                      onButtonClick={this.setWriteAccessToProject}
                      width={79}
                    />
                  ) : userAccess === UserAccess.Write ? (
                    <ButtonTooltip
                      additionalClassName={`${styles.collaborator_button} ${
                        styles.blue_button
                      }`}
                      icon={<Icon type="share-write" />}
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
                  {userAccess === UserAccess.Owner && false ? (
                    <ButtonTooltip
                      additionalClassName={`${styles.collaborator_button} ${
                        styles.blue_button
                      }`}
                      icon={<Icon type="share-change" />}
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
                        icon={<Icon type="share-read" />}
                        toolTipContent={'Read only'}
                        width={79}
                      />
                    ) : userAccess === UserAccess.Write ? (
                      <ButtonTooltip
                        additionalClassName={`${styles.collaborator_button} ${
                          styles.without_active_button
                        }`}
                        icon={<Icon type="share-write" />}
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
                      icon={<Icon type="share-delete" />}
                      toolTipContent={'Delete'}
                      onButtonClick={this.removeAccessToProject}
                      width={59}
                    />
                  ) : (
                    ''
                  )}
                </div>
              </>
            );
          })()}
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

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    isChangingAccess: selectIsChangingUserAccess(state, localProps.user.id!),
    isRemovingAccess: selectIsRemovingUserAccess(state, localProps.user.id!),
  };
};

export default connect(mapStateToProps)(CollaboratorItem);
