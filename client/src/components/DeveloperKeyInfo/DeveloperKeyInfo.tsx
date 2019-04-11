import { bind } from 'decko';
import moment from 'moment';
import * as React from 'react';
import { connect } from 'react-redux';

import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';
import Icon from 'components/shared/Icon/Icon';
import { CurrentUser } from 'models/User';
import { IApplicationState } from 'store/store';
import { selectCurrentUser } from 'store/user';

import styles from './DeveloperKeyInfo.module.css';

interface ILocalProps {
  close?: React.ReactNode;
}

interface IPropsFromState {
  user: CurrentUser;
}

type AllProps = ILocalProps & IPropsFromState;

class DeveloperKeyInfo extends React.PureComponent<AllProps> {
  public render() {
    const { user, close } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.header}>
          <div className={styles.title}>Your Developer Key</div>
          <div className={styles.close}>{close}</div>
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

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    user: selectCurrentUser(state)!,
  };
};

export default connect(mapStateToProps)(DeveloperKeyInfo);
