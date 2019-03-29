import React from 'react';
import {
  Route,
  RouteComponentProps,
  Switch,
  withRouter,
} from 'react-router-dom';

import Login from 'components/Login/Login';
import routes from 'routes';

import styles from './AnonymousLayout.module.css';
import AnonymousLayoutHeader from './AnonymousLayoutHeader/AnonymousLayoutHeader';
import { Footer } from './Footer/Footer';

class AnonymousLayout extends React.Component<RouteComponentProps> {
  public render() {
    return (
      <div className={styles.layout}>
        <div className={styles.header}>
          <AnonymousLayoutHeader />
        </div>
        <div className={styles.content}>
          <Switch>
            <Route component={Login} />
          </Switch>
        </div>
        <div className={styles.footer}>
          <Footer />
        </div>
      </div>
    );
  }
}

export default withRouter(AnonymousLayout);
