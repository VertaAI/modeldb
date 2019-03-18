import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from '../../routes';
import styles from './ExpSubMenu.module.css';

interface IProps {
  projectId: string;
  active: string;
}

export default class ExpSubMenu extends React.Component<IProps> {
  public render() {
    const { projectId, active } = this.props;
    return active === 'charts' ? (
      <div>
        <nav className={styles.nav_menu}>
          <Link to={routes.expirementRuns.getRedirectPath({ projectId })}>Experiment Runs</Link>
          <Link className={styles.active} to={routes.charts.getRedirectPath({ projectId })}>
            Charts
          </Link>
        </nav>
      </div>
    ) : (
      <div>
        <nav className={styles.nav_menu}>
          <Link className={styles.active} to={routes.expirementRuns.getRedirectPath({ projectId })}>
            Experiment Runs
          </Link>
          <Link to={routes.charts.getRedirectPath({ projectId })}>Charts</Link>
        </nav>
      </div>
    );
  }
}
