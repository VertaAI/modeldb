import { bind } from 'decko';
import * as React from 'react';
import Avatar from 'react-avatar';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import User from 'models/User';
import routes from 'routes';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { logoutUser } from 'store/user/actions';

import styles from './UserBar.module.css';

interface ILocalState {
  isOpened: boolean;
}

interface IPropsFromState {
  user: User | null;
}

type AllProps = IConnectedReduxProps & IPropsFromState;

class UserBar extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { isOpened: false };

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  public render() {
    const user = this.props.user;

    return (
      <div className={styles.root}>
        <div className={styles.user_bar} onClick={this.toggleMenu}>
          <Avatar
            name={user ? user.name : ''}
            color="var(--bg-color3)"
            fgColor="black"
            round={true}
            size="36"
            textSizeRatio={36 / 16}
            style={{ fontFamily: 'Roboto', fontWeight: '400' }}
            src={user ? user.picture : ''}
          />
          <div className={styles.menu_arrow}>
            <i className="fa fa-caret-down" />
          </div>
        </div>
        {this.state.isOpened ? (
          <div className={styles.drop_down}>
            <div className={styles.menu_header}>
              <Avatar
                name={user ? user.name : ''}
                color="var(--bg-color2)"
                fgColor="var(--bg-color3)"
                round={true}
                size="48"
                textSizeRatio={36 / 16}
                style={{ fontFamily: 'Roboto', fontWeight: '400' }}
                src={user ? user.picture : ''}
              />
              <div className={styles.menu_header_user_name}>
                {user ? user.name : ''}
              </div>
            </div>
            <div className={styles.menu_item}>
              <Link
                onClick={this.toggleMenu}
                to={routes.settings.getRedirectPath({})}
              >
                Settings
              </Link>
            </div>
            <div className={styles.menu_item}>
              <Link
                onClick={this.logout}
                to={routes.mainPage.getRedirectPath({})}
              >
                Log out
              </Link>
            </div>
          </div>
        ) : (
          ''
        )}
      </div>
    );
  }

  @bind
  private handleClickOutside() {
    this.setState({ isOpened: false });
  }

  @bind
  private toggleMenu(): void {
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  }

  @bind
  private logout(): void {
    this.toggleMenu();
    this.props.dispatch(logoutUser());
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user,
});

export default connect(mapStateToProps)(onClickOutside(UserBar));
