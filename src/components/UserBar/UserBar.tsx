import User from 'models/User';
import * as React from 'react';
import Avatar from 'react-avatar';
import onClickOutside from 'react-onclickoutside';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { IApplicationState, IConnectedReduxProps } from '../../store/store';
import styles from './UserBar.module.css';

interface ILocalState {
  isOpened: boolean;
}

interface IPropsFromState {
  user: User | null;
}

type AllProps = IConnectedReduxProps & IPropsFromState;

class UserBar extends React.Component<AllProps, ILocalState> {
  public constructor(props: AllProps) {
    super(props);
    this.state = { isOpened: false };
    this.toggleMenu = this.toggleMenu.bind(this);
  }

  public render() {
    const user = this.props.user;

    return (
      <div className={styles.root}>
        <div className={styles.user_bar} onClick={this.toggleMenu}>
          <Avatar
            name={user ? user.name : ''}
            color="white"
            fgColor="black"
            round={true}
            size="36"
            textSizeRatio={36 / 16}
            style={{ fontFamily: 'Roboto', fontWeight: '400' }}
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
                fgColor="white"
                round={true}
                size="48"
                textSizeRatio={36 / 16}
                style={{ fontFamily: 'Roboto', fontWeight: '400' }}
              />
              <div>
                <div className={styles.menu_header_user_name}>{user ? user.name : ''}</div>
              </div>
            </div>
            <div className={styles.menu_item}>
              <Link onClick={this.toggleMenu} to="/settings">
                Settings
              </Link>
            </div>
            <div className={styles.menu_item}>
              <Link onClick={this.toggleMenu} to="/logout">
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

  public componentDidMount() {
    this.setState({ isOpened: false });
  }

  public handleClickOutside(ev: MouseEvent) {
    this.setState({ isOpened: false });
  }

  private toggleMenu(): void {
    this.setState(prevState => ({ isOpened: !prevState.isOpened }));
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect(mapStateToProps)(onClickOutside(UserBar));
