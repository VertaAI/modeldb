import User from 'models/User';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { IApplicationState } from 'store/store';
import UserBar from '../UserBar/UserBar';
import styles from './LayoutHeader.module.css';
import logo from './Verta logo.svg';

interface IPropsFromState {
  user: User | undefined;
}

class LayoutHeader extends React.Component<IPropsFromState, {}> {
  public render() {
    return (
      <header className={styles.header}>
        <div className={styles.logo}>Logo</div>
        <nav className={styles.nav_menu}>
          <Link to="/">About</Link>
          <Link to="/project">Blog</Link>
          <Link to="/model">Settings</Link>
        </nav>
        <div className={styles.user_bar}>
          <UserBar />
        </div>
      </header>
    );
  }
}

const mapStateToProps = ({ layout }: IApplicationState) => ({
  user: layout.user
});

export default connect<IPropsFromState, {}, {}, IApplicationState>(mapStateToProps)(LayoutHeader);
