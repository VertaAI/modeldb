import React from 'react';
import Avatar from 'react-avatar';
import Scrollbars, { positionValues } from 'react-custom-scrollbars';
import { UserAccess } from '../../../models/Project';
import User from '../../../models/User';
import { ButtonTooltip } from '../ButtonTooltip/ButtonTooltip';
import share_change_icon from '../images/share-change-icon.svg';
import share_delete_icon from '../images/share-del-icon.svg';
import share_read_icon from '../images/share-r-icon.svg';
import share_write_icon from '../images/share-wr-icon.svg';
import styles from './CollaboratorsTab.module.css';

interface ILocalProps {
  collaborators: Map<User, UserAccess>;
}

interface ILocalState {
  shadowTopOpacity: number;
  shadowBottomOpacity: number;
}

export class CollaboratorsTab extends React.Component<ILocalProps, ILocalState> {
  constructor(props: ILocalProps) {
    super(props);

    this.state = {
      shadowBottomOpacity: 0,
      shadowTopOpacity: 0
    };

    this.handleUpdate = this.handleUpdate.bind(this);
  }

  public render() {
    return (
      <div style={{ position: 'relative' }}>
        <Scrollbars
          autoHeightMax={'calc(100vh - 200px)'}
          autoHeight={true}
          style={{ margin: '20px 16px 0px 40px', width: 'auto' }}
          onUpdate={this.handleUpdate}
        >
          {Array.from(this.props.collaborators.entries()).map((value: [User, UserAccess], index: number) => {
            const [user, userAccess] = value;
            return (
              <div key={index} className={styles.collaborator}>
                <Avatar
                  name={user ? user.name : ''}
                  round={true}
                  size="40"
                  textSizeRatio={40 / 16}
                  className={styles.user_avatar}
                  src={user ? user.picture : ''}
                />
                <div className={styles.user_name}>
                  <div>{user.name}</div>
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
                        width={104}
                      />
                    </div>
                  ) : userAccess === UserAccess.Read ? (
                    <div className={styles.collaborator_buttons}>
                      <ButtonTooltip
                        additionalClassName={`${styles.collaborator_button} ${styles.blue_button}`}
                        imgSrc={share_read_icon}
                        toolTipContent={'Read only'}
                        width={79}
                      />
                      <div />
                      <ButtonTooltip
                        additionalClassName={`${styles.collaborator_button} ${styles.red_button}`}
                        imgSrc={share_delete_icon}
                        toolTipContent={'Delete'}
                        width={59}
                      />
                    </div>
                  ) : userAccess === UserAccess.Write ? (
                    <div className={styles.collaborator_buttons}>
                      <ButtonTooltip
                        additionalClassName={`${styles.collaborator_button} ${styles.blue_button}`}
                        imgSrc={share_write_icon}
                        toolTipContent={'Read / Write'}
                        width={93}
                      />
                      <div />
                      <ButtonTooltip
                        additionalClassName={`${styles.collaborator_button} ${styles.red_button}`}
                        imgSrc={share_delete_icon}
                        toolTipContent={'Delete'}
                        width={59}
                      />
                    </div>
                  ) : (
                    ''
                  )}
                </div>
              </div>
            );
          })}
        </Scrollbars>
        <div
          className={styles.shadowTop}
          style={{ opacity: this.state.shadowTopOpacity, visibility: this.state.shadowTopOpacity === 1 ? 'visible' : 'collapse' }}
        />
        <div
          className={styles.shadowBottom}
          style={{ opacity: this.state.shadowBottomOpacity, visibility: this.state.shadowBottomOpacity === 1 ? 'visible' : 'collapse' }}
        />
      </div>
    );
  }
  private handleUpdate(values: positionValues) {
    const { scrollTop, scrollHeight, clientHeight } = values;
    const shadowTopOpacity1 = (1 / 20) * Math.min(scrollTop, 20);
    const bottomScrollTop = scrollHeight - clientHeight;
    const shadowBottomOpacity1 = (1 / 20) * (bottomScrollTop - Math.max(scrollTop, bottomScrollTop - 20));
    this.setState({ ...this.state, shadowBottomOpacity: shadowBottomOpacity1, shadowTopOpacity: shadowTopOpacity1 });
  }
}
