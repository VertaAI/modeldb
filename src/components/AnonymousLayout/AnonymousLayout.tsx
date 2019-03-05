import React from 'react';
import { Route, RouteComponentProps, Switch, withRouter } from 'react-router-dom';
import Callback from '../../components/Callback/Callback';
import { Footer } from '../../components/Footer/Footer';
import Login from '../../components/Login/Login';
import AnonymousLayoutHeader from '../AnonymousLayoutHeader/AnonymousLayoutHeader';
import styles from './AnonymousLayout.module.css';

class AnonymousLayout extends React.Component<RouteComponentProps> {
  public render() {
    return (
      <div className={styles.layout}>
        <div className={styles.header}>
          <AnonymousLayoutHeader />
        </div>
        <div className={styles.content}>
          <Switch>
            <Route path={'/callback'} component={Callback} />
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
