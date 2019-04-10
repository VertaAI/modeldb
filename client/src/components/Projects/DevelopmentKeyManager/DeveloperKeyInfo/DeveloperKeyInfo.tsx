import { bind } from 'decko';
import moment from 'moment';
import * as React from 'react';

import Icon from 'components/shared/Icon/Icon';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';
import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import User from 'models/User';

import styles from './DeveloperKeyInfo.module.css';

interface ILocalProps {
  user: User;
  onHide: () => void;
}

class DeveloperKeyInfo extends React.PureComponent<ILocalProps> {
  public render() {
    const { user, onHide } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.header}>
          <div className={styles.title}>Your Developer Key</div>
          <Icon type="close" className={styles.close_icon} onClick={onHide} />
        </div>
        <div className={styles.content}>
          <div className={styles.user_info}>
            <this.UserInfoItem label="Name">
              {user.getNameOrEmail()}
            </this.UserInfoItem>
            <this.UserInfoItem label="Last loggedin">
              {user.dateLastLoggedIn
                ? moment(user.dateLastLoggedIn).format('DD MMM YYYY')
                : '-'}
            </this.UserInfoItem>
          </div>
          <div className={styles.developer_key}>
            <Icon type="key" className={styles.developer_key_icon} />
            <span className={styles.developer_key_value}>
              {user.developerKey}
            </span>
            <div className={styles.developer_key_copy}>
              <CopyToClipboard text={user.developerKey}>
                {onCopy => (
                  <ButtonLikeText onClick={onCopy}>Copy</ButtonLikeText>
                )}
              </CopyToClipboard>
            </div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private UserInfoItem({
    label,
    children,
  }: {
    label: string;
    children: string;
  }) {
    return (
      <>
        <div className={styles.user_info_item_label}>{label}</div>
        <div className={styles.user_info_item_content}>{children}</div>
      </>
    );
  }
}

export default DeveloperKeyInfo;
