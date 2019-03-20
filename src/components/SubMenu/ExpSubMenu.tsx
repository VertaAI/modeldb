import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from '../../routes';
import styles from './ExpSubMenu.module.css';

export default class ExpSubMenu extends React.Component {
  public render() {
    return (
      <div>
        <nav className={styles.nav_menu}>
          <Link to={routes.mainPage.getPath()}>Experiment Runs</Link>
          <Link className={styles.active} to={routes.mainPage.getPath()}>
            Charts
          </Link>
        </nav>
      </div>
    );
  }
}
